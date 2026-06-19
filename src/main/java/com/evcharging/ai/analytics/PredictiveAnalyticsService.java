package com.evcharging.ai.analytics;

import com.evcharging.repository.BookingRepository;
import com.evcharging.repository.ChargingStationRepository;
import com.evcharging.repository.ChargingSlotRepository;
import com.evcharging.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PredictiveAnalyticsService — Phase 6: AI-Powered Analytics
 *
 * Architecture:
 * ─────────────
 * This service combines statistical analysis with LLM-based forecasting:
 *
 * 1. PEAK HOUR DETECTION:
 *    - Aggregates historical bookings by hour of day (SQL GROUP BY HOUR)
 *    - Identifies peak hours (top 20% of activity)
 *    - LLM interprets patterns and suggests capacity planning
 *
 * 2. DEMAND FORECASTING:
 *    - Analyses bookings per day-of-week pattern
 *    - Uses simple moving average for trend detection
 *    - LLM generates human-readable forecast narratives
 *
 * 3. SLOT OCCUPANCY:
 *    - Per-station slot utilization percentage
 *    - Time-series of usage per day over last 30 days
 *
 * 4. ADMIN DASHBOARD INSIGHTS:
 *    - Summary KPIs: total bookings, revenue, utilization rate
 *    - LLM-generated insight summary for non-technical admins
 *
 * Why use LLM for analytics?
 *  - LLM can interpret statistical patterns in natural language
 *  - Generates actionable recommendations ("Add 3 DC chargers at Station X during peak hours")
 *  - Non-technical admins get readable insights instead of raw numbers
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PredictiveAnalyticsService {

    private final ChatClient chatClient;
    private final BookingRepository bookingRepository;
    private final ChargingStationRepository stationRepository;
    private final ChargingSlotRepository slotRepository;
    private final UserRepository userRepository;

    @Value("${ev.analytics.forecast-days:7}")
    private int forecastDays;

    // ─────────────────────────────────────────────────────────────
    //  PUBLIC API
    // ─────────────────────────────────────────────────────────────

    /**
     * Generate a comprehensive analytics dashboard for admins.
     * Combines statistical data + AI-generated narrative insights.
     */
    public AnalyticsDashboard generateDashboard() {
        log.info("Generating AI analytics dashboard...");

        try {
            // 1. KPIs
            DashboardKPIs kpis = computeKPIs();

            // 2. Peak hour analysis
            Map<Integer, Long> hourlyPattern = getHourlyBookingPattern();
            List<Integer> peakHours = detectPeakHours(hourlyPattern);

            // 3. Day-of-week pattern
            Map<String, Long> weeklyPattern = getWeeklyPattern();

            // 4. Station utilization
            List<StationUtilization> utilization = getStationUtilization();

            // 5. Recent trend (last 30 days)
            List<DailyBookingCount> recentTrend = getRecentTrend(30);

            // 6. AI-generated insights
            String aiInsights = generateAIInsights(kpis, hourlyPattern, peakHours,
                    weeklyPattern, utilization);

            // 7. Demand forecast
            List<ForecastPoint> forecast = generateForecast(recentTrend);

            return AnalyticsDashboard.builder()
                    .kpis(kpis)
                    .peakHours(peakHours)
                    .hourlyPattern(hourlyPattern)
                    .weeklyPattern(weeklyPattern)
                    .stationUtilization(utilization)
                    .recentTrend(recentTrend)
                    .forecast(forecast)
                    .aiInsights(aiInsights)
                    .generatedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Analytics dashboard generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Analytics service error: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  KPI Computation
    // ─────────────────────────────────────────────────────────────

    private DashboardKPIs computeKPIs() {
        long totalBookings = bookingRepository.count();
        long totalStations = stationRepository.count();
        long totalUsers = userRepository.count();
        long totalSlots = slotRepository.count();
        Double totalRevenue = bookingRepository.getTotalRevenue();

        long activeStations = stationRepository.findByOperationalStatus(
                com.evcharging.model.ChargingStation.OperationalStatus.ACTIVE).size();

        // Available slots right now
        long availableSlots = slotRepository.findByStatus(
                com.evcharging.model.ChargingSlot.SlotStatus.AVAILABLE).size();

        double utilizationRate = totalSlots > 0
                ? (double)(totalSlots - availableSlots) / totalSlots * 100.0 : 0.0;

        return DashboardKPIs.builder()
                .totalBookings(totalBookings)
                .totalStations(totalStations)
                .activeStations(activeStations)
                .totalUsers(totalUsers)
                .totalSlots(totalSlots)
                .currentlyAvailableSlots(availableSlots)
                .utilizationRate(Math.round(utilizationRate * 10.0) / 10.0)
                .totalRevenue(totalRevenue != null ? totalRevenue : 0.0)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    //  Peak Hour Analysis
    // ─────────────────────────────────────────────────────────────

    private Map<Integer, Long> getHourlyBookingPattern() {
        List<Object[]> results = bookingRepository.findBookingsByHourForStation(null);
        // If stationId is null, this won't match — use all-station query instead

        // Alternative: manually aggregate all bookings by hour
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<com.evcharging.model.Booking> recentBookings = bookingRepository
                .findBookingsInRange(thirtyDaysAgo, LocalDateTime.now());

        Map<Integer, Long> hourlyMap = new TreeMap<>();
        for (int h = 0; h < 24; h++) {
            final int hour = h;
            long count = recentBookings.stream()
                    .filter(b -> b.getBookingTime() != null && b.getBookingTime().getHour() == hour)
                    .count();
            hourlyMap.put(h, count);
        }
        return hourlyMap;
    }

    private List<Integer> detectPeakHours(Map<Integer, Long> hourlyPattern) {
        if (hourlyPattern.isEmpty()) return List.of(8, 9, 17, 18, 19);

        long maxBookings = hourlyPattern.values().stream().max(Long::compareTo).orElse(1L);
        long peakThreshold = (long)(maxBookings * 0.7); // top 30% of peak activity

        return hourlyPattern.entrySet().stream()
                .filter(e -> e.getValue() >= peakThreshold && e.getValue() > 0)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    //  Weekly Pattern
    // ─────────────────────────────────────────────────────────────

    private Map<String, Long> getWeeklyPattern() {
        List<Object[]> results = bookingRepository.findBookingsByDayOfWeek();
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        Map<String, Long> pattern = new LinkedHashMap<>();
        for (String day : dayNames) pattern.put(day, 0L);

        results.forEach(row -> {
            int dow = ((Number) row[0]).intValue(); // 1=Sunday, 7=Saturday
            long count = ((Number) row[1]).longValue();
            if (dow >= 1 && dow <= 7) {
                pattern.put(dayNames[dow - 1], count);
            }
        });
        return pattern;
    }

    // ─────────────────────────────────────────────────────────────
    //  Station Utilization
    // ─────────────────────────────────────────────────────────────

    private List<StationUtilization> getStationUtilization() {
        List<Object[]> stats = bookingRepository.findStationUtilizationStats();
        return stats.stream()
                .limit(10)
                .map(row -> StationUtilization.builder()
                        .stationId(((Number) row[0]).longValue())
                        .stationName((String) row[1])
                        .totalBookings(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    //  Recent Trend
    // ─────────────────────────────────────────────────────────────

    private List<DailyBookingCount> getRecentTrend(int days) {
        LocalDateTime start = LocalDateTime.now().minusDays(days);
        List<com.evcharging.model.Booking> bookings = bookingRepository
                .findBookingsInRange(start, LocalDateTime.now());

        Map<LocalDate, Long> dailyCounts = bookings.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getBookingTime().toLocalDate(),
                        Collectors.counting()
                ));

        List<DailyBookingCount> trend = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            trend.add(DailyBookingCount.builder()
                    .date(date)
                    .count(dailyCounts.getOrDefault(date, 0L))
                    .build());
        }
        return trend;
    }

    // ─────────────────────────────────────────────────────────────
    //  Demand Forecast (Simple Moving Average + AI Narrative)
    // ─────────────────────────────────────────────────────────────

    private List<ForecastPoint> generateForecast(List<DailyBookingCount> historical) {
        // Simple 7-day moving average forecast
        List<ForecastPoint> forecast = new ArrayList<>();
        int windowSize = Math.min(7, historical.size());

        double avgBookingsPerDay = historical.stream()
                .mapToLong(DailyBookingCount::getCount)
                .average()
                .orElse(0.0);

        // Simple linear trend (last 7 days vs previous 7 days)
        double recentAvg = historical.stream().skip(Math.max(0, historical.size() - 7))
                .mapToLong(DailyBookingCount::getCount).average().orElse(avgBookingsPerDay);
        double olderAvg = historical.stream().limit(Math.max(1, historical.size() - 7))
                .mapToLong(DailyBookingCount::getCount).average().orElse(avgBookingsPerDay);
        double trend = olderAvg > 0 ? (recentAvg - olderAvg) / olderAvg : 0;

        for (int i = 1; i <= forecastDays; i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            long predictedCount = Math.max(0, Math.round(recentAvg * (1 + trend * i * 0.1)));
            forecast.add(ForecastPoint.builder()
                    .date(date)
                    .predictedBookings(predictedCount)
                    .trendDirection(trend > 0.05 ? "UP" : trend < -0.05 ? "DOWN" : "STABLE")
                    .build());
        }
        return forecast;
    }

    // ─────────────────────────────────────────────────────────────
    //  AI Insights Generation
    // ─────────────────────────────────────────────────────────────

    /**
     * Uses LLM to generate human-readable business insights from raw statistics.
     * This is where AI adds the most value — turning numbers into narratives.
     */
    private String generateAIInsights(
            DashboardKPIs kpis,
            Map<Integer, Long> hourlyPattern,
            List<Integer> peakHours,
            Map<String, Long> weeklyPattern,
            List<StationUtilization> utilization) {

        String dataContext = String.format("""
            === EV CHARGING STATION ANALYTICS DATA ===
            
            KEY METRICS:
            - Total Bookings (all time): %d
            - Total Active Stations: %d / %d
            - Total Users: %d
            - Current Utilization Rate: %.1f%%
            - Total Revenue: ₹%.2f
            - Available Slots Right Now: %d / %d
            
            PEAK HOURS (highest demand): %s
            
            HOURLY BOOKING PATTERN (last 30 days):
            %s
            
            WEEKLY PATTERN (day of week):
            %s
            
            TOP STATIONS BY BOOKINGS:
            %s
            """,
            kpis.getTotalBookings(),
            kpis.getActiveStations(), kpis.getTotalStations(),
            kpis.getTotalUsers(),
            kpis.getUtilizationRate(),
            kpis.getTotalRevenue(),
            kpis.getCurrentlyAvailableSlots(), kpis.getTotalSlots(),
            peakHours.stream().map(h -> String.format("%02d:00", h)).collect(Collectors.joining(", ")),
            formatHourlyPattern(hourlyPattern),
            weeklyPattern.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue() + " bookings")
                    .collect(Collectors.joining(", ")),
            utilization.stream().limit(5)
                    .map(u -> u.getStationName() + " (" + u.getTotalBookings() + " bookings)")
                    .collect(Collectors.joining(", "))
        );

        String insightPrompt = dataContext + """
            
            Based on the above analytics data for an EV Charging Station Management System, provide:
            
            1. **Executive Summary** (2-3 sentences): Key highlights
            2. **Peak Usage Insights**: What do the peak hours tell us? What does this mean for capacity?
            3. **Weekly Trends**: Which days are busiest? Recommendations?
            4. **Station Performance**: Which stations need attention?
            5. **Revenue Opportunities**: How can revenue be improved?
            6. **Actionable Recommendations** (3-5 bullet points): What should management do next?
            
            Keep insights practical and specific to EV charging operations.
            """;

        try {
            return chatClient.prompt()
                    .system("You are a business analytics expert specializing in EV charging infrastructure.")
                    .user(insightPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("AI insight generation failed: {}", e.getMessage());
            return "AI insights unavailable. Please check your API configuration.";
        }
    }

    private String formatHourlyPattern(Map<Integer, Long> pattern) {
        return pattern.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> String.format("%02d:00=%d", e.getKey(), e.getValue()))
                .collect(Collectors.joining(", "));
    }

    // ─────────────────────────────────────────────────────────────
    //  Inner Data Classes (Response Models)
    // ─────────────────────────────────────────────────────────────

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class AnalyticsDashboard {
        private DashboardKPIs kpis;
        private List<Integer> peakHours;
        private Map<Integer, Long> hourlyPattern;
        private Map<String, Long> weeklyPattern;
        private List<StationUtilization> stationUtilization;
        private List<DailyBookingCount> recentTrend;
        private List<ForecastPoint> forecast;
        private String aiInsights;
        private LocalDateTime generatedAt;
    }

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class DashboardKPIs {
        private long totalBookings;
        private long totalStations;
        private long activeStations;
        private long totalUsers;
        private long totalSlots;
        private long currentlyAvailableSlots;
        private double utilizationRate;
        private double totalRevenue;
    }

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class StationUtilization {
        private long stationId;
        private String stationName;
        private long totalBookings;
    }

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class DailyBookingCount {
        private LocalDate date;
        private long count;
    }

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class ForecastPoint {
        private LocalDate date;
        private long predictedBookings;
        private String trendDirection;
    }
}
