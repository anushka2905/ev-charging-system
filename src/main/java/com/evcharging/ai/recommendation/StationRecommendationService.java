package com.evcharging.ai.recommendation;

import com.evcharging.dto.StationSummaryDTO;
import com.evcharging.exception.AIServiceException;
import com.evcharging.model.ChargingSlot;
import com.evcharging.model.ChargingStation;
import com.evcharging.repository.ChargingStationRepository;
import com.evcharging.service.ChargingStationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * StationRecommendationService — Phase 3: AI-Powered Recommendation Engine
 *
 * Architecture:
 * ─────────────
 * This service uses a HYBRID approach:
 *
 * Step 1 — BUSINESS LOGIC SCORING (deterministic, fast):
 *   Each candidate station is scored on 3 weighted dimensions:
 *   • Distance Score    (weight 0.4): closer = higher score
 *   • Availability Score (weight 0.3): more free slots = higher score
 *   • Rating Score      (weight 0.3): higher rating = higher score
 *   Total score range: 0–100
 *
 * Step 2 — AI REASONING (generative, natural language):
 *   Top-N stations are passed to the LLM which:
 *   • Interprets the user's request context
 *   • Ranks stations considering charger type preference
 *   • Generates a human-readable explanation for each recommendation
 *
 * Why hybrid?
 *   Pure AI-based ranking would be slow and expensive.
 *   Pure algorithmic ranking gives no explanation.
 *   Hybrid gives fast, explainable, context-aware results.
 *
 * Configuration (from application.properties):
 *   ev.recommendation.max-distance-km=50.0
 *   ev.recommendation.max-results=5
 *   ev.recommendation.weight-distance=0.4
 *   ev.recommendation.weight-availability=0.3
 *   ev.recommendation.weight-rating=0.3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StationRecommendationService {

    private final ChatClient chatClient;
    private final ChargingStationRepository stationRepository;

    @Value("${ev.recommendation.max-distance-km:50.0}")
    private double maxDistanceKm;

    @Value("${ev.recommendation.max-results:5}")
    private int maxResults;

    @Value("${ev.recommendation.weight-distance:0.4}")
    private double weightDistance;

    @Value("${ev.recommendation.weight-availability:0.3}")
    private double weightAvailability;

    @Value("${ev.recommendation.weight-rating:0.3}")
    private double weightRating;

    // ─────────────────────────────────────────────────────────────
    //  PUBLIC API
    // ─────────────────────────────────────────────────────────────

    /**
     * Get AI-powered station recommendations.
     *
     * @param userLat         User's current latitude
     * @param userLon         User's current longitude
     * @param preferredType   Preferred charger type (nullable = no preference)
     * @param userContext     Natural language context ("I need fast charging for a long trip")
     * @return List of recommended stations with AI-generated explanations
     */
    public List<StationSummaryDTO> recommend(
            double userLat,
            double userLon,
            ChargingSlot.ChargerType preferredType,
            String userContext) {

        log.info("Recommendation request: lat={} lon={} type={} context={}",
                userLat, userLon, preferredType, userContext);

        try {
            // Step 1: Get candidate stations within bounding box
            List<ChargingStation> candidates = getCandidateStations(userLat, userLon);
            if (candidates.isEmpty()) {
                log.warn("No candidate stations found within {} km", maxDistanceKm);
                return List.of();
            }

            // Step 2: Business logic scoring + filter by charger type
            List<StationSummaryDTO> scored = scoreAndFilter(candidates, userLat, userLon, preferredType);
            if (scored.isEmpty()) {
                return List.of();
            }

            // Step 3: Take top candidates for AI reasoning
            List<StationSummaryDTO> topCandidates = scored.stream()
                    .limit(maxResults * 2L) // give AI more choices
                    .collect(Collectors.toList());

            // Step 4: AI reasoning — enrich with explanations
            return enrichWithAIReasoning(topCandidates, preferredType, userContext, userLat, userLon);

        } catch (AIServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Recommendation engine error: {}", e.getMessage(), e);
            throw new AIServiceException("Recommendation engine failed: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  STEP 1: Candidate Selection
    // ─────────────────────────────────────────────────────────────

    private List<ChargingStation> getCandidateStations(double lat, double lon) {
        double deltaLat = maxDistanceKm / 111.0;
        double deltaLon = maxDistanceKm / (111.0 * Math.cos(Math.toRadians(lat)));

        return stationRepository.findWithinBoundingBox(
                lat - deltaLat, lat + deltaLat,
                lon - deltaLon, lon + deltaLon);
    }

    // ─────────────────────────────────────────────────────────────
    //  STEP 2: Business Logic Scoring
    // ─────────────────────────────────────────────────────────────

    private List<StationSummaryDTO> scoreAndFilter(
            List<ChargingStation> stations,
            double userLat, double userLon,
            ChargingSlot.ChargerType preferredType) {

        // Find max values for normalization
        double maxRating = stations.stream().mapToDouble(ChargingStation::getRating).max().orElse(5.0);
        if (maxRating == 0) maxRating = 5.0;
        int maxSlots = stations.stream().mapToInt(ChargingStation::getTotalSlots).max().orElse(1);
        if (maxSlots == 0) maxSlots = 1;

        final double maxR = maxRating;
        final int maxS = maxSlots;

        return stations.stream()
                .map(station -> {
                    double distKm = ChargingStationServiceImpl.haversineKm(
                            userLat, userLon, station.getLatitude(), station.getLongitude());
                    if (distKm > maxDistanceKm) return null; // out of range

                    int availableSlots = stationRepository.countAvailableSlots(station.getId());

                    // Filter by charger type if specified
                    if (preferredType != null && station.getChargerTypes() != null) {
                        if (!station.getChargerTypes().contains(preferredType.name())) {
                            return null; // doesn't support requested charger type
                        }
                    }

                    // Score calculations (all normalized to 0-1)
                    double distanceScore = 1.0 - (distKm / maxDistanceKm);           // farther = lower
                    double availabilityScore = (double) availableSlots / maxS;        // more slots = higher
                    double ratingScore = station.getRating() / maxR;                  // higher rating = higher

                    // Weighted composite score
                    double totalScore = (weightDistance * distanceScore
                                      + weightAvailability * availabilityScore
                                      + weightRating * ratingScore) * 100.0;

                    return StationSummaryDTO.builder()
                            .id(station.getId())
                            .name(station.getName())
                            .city(station.getCity())
                            .state(station.getState())
                            .latitude(station.getLatitude())
                            .longitude(station.getLongitude())
                            .availableSlots(availableSlots)
                            .totalSlots(station.getTotalSlots())
                            .pricePerUnit(station.getPricePerUnit())
                            .chargerTypes(station.getChargerTypes())
                            .rating(station.getRating())
                            .operationalStatus(station.getOperationalStatus() != null
                                    ? station.getOperationalStatus().name() : "ACTIVE")
                            .operator(station.getOperator())
                            .distanceKm(Math.round(distKm * 10.0) / 10.0)
                            .recommendationScore(Math.round(totalScore * 10.0) / 10.0)
                            .build();
                })
                .filter(dto -> dto != null)
                .sorted(Comparator.comparingDouble(StationSummaryDTO::getRecommendationScore).reversed())
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    //  STEP 3: AI Reasoning
    // ─────────────────────────────────────────────────────────────

    /**
     * Passes the pre-scored stations to the LLM and asks it to:
     *  1. Rank them considering the user's context
     *  2. Generate a one-sentence explanation per station
     */
    private List<StationSummaryDTO> enrichWithAIReasoning(
            List<StationSummaryDTO> candidates,
            ChargingSlot.ChargerType preferredType,
            String userContext,
            double userLat,
            double userLon) {

        // Build the station list for the AI prompt
        StringBuilder stationList = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            StationSummaryDTO s = candidates.get(i);
            stationList.append(String.format(
                "%d. %s | %s, %s | %.1f km away | %d/%d slots free | ₹%.1f/kWh | " +
                "Chargers: %s | Rating: %.1f | Score: %.1f\n",
                i + 1, s.getName(), s.getCity(), s.getState(),
                s.getDistanceKm(), s.getAvailableSlots(), s.getTotalSlots(),
                s.getPricePerUnit(), s.getChargerTypes() != null ? s.getChargerTypes() : "AC",
                s.getRating(), s.getRecommendationScore()
            ));
        }

        String aiPrompt = String.format("""
            You are an EV charging station recommendation assistant.
            
            User's context: "%s"
            User's preferred charger type: %s
            User location: (%.4f, %.4f)
            
            Candidate stations (pre-scored by distance, availability, and rating):
            %s
            
            For each station, provide ONE concise sentence explaining WHY it's recommended 
            (or not ideal) for this user. Focus on what matters most for their stated context.
            
            Format your response EXACTLY as:
            STATION_1: <explanation>
            STATION_2: <explanation>
            STATION_3: <explanation>
            (continue for all stations)
            """,
            userContext != null ? userContext : "Find the best available charging station",
            preferredType != null ? preferredType.getDisplayName() : "No preference",
            userLat, userLon,
            stationList.toString()
        );

        try {
            String aiResponse = chatClient.prompt()
                    .user(aiPrompt)
                    .call()
                    .content();

            // Parse AI explanations back into DTOs
            return parseAIExplanations(candidates, aiResponse);

        } catch (Exception e) {
            log.warn("AI reasoning failed, returning scored results without explanations: {}", e.getMessage());
            // Graceful degradation — return algorithmic results without AI explanation
            candidates.forEach(s -> s.setRecommendationReason(
                    String.format("%.1f km away, %d slots available, rated %.1f/5",
                            s.getDistanceKm(), s.getAvailableSlots(), s.getRating())));
            return candidates;
        }
    }

    /** Parse the AI's STATION_N: explanation format */
    private List<StationSummaryDTO> parseAIExplanations(
            List<StationSummaryDTO> candidates, String aiResponse) {

        String[] lines = aiResponse.split("\n");
        for (String line : lines) {
            for (int i = 0; i < candidates.size(); i++) {
                String marker = "STATION_" + (i + 1) + ":";
                if (line.toUpperCase().startsWith(marker)) {
                    String explanation = line.substring(marker.length()).trim();
                    candidates.get(i).setRecommendationReason(explanation);
                    break;
                }
            }
        }

        // Fallback for any station that didn't get an explanation
        candidates.stream()
                .filter(s -> s.getRecommendationReason() == null || s.getRecommendationReason().isBlank())
                .forEach(s -> s.setRecommendationReason(
                        String.format("%.1f km away, %d slots available", s.getDistanceKm(), s.getAvailableSlots())));

        return candidates;
    }
}
