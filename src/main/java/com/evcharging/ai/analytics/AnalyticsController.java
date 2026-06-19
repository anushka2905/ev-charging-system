package com.evcharging.ai.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AnalyticsController — Phase 6: Predictive Analytics API
 *
 * GET /api/ai/analytics/dashboard — Full AI analytics dashboard (ADMIN only)
 * GET /api/ai/analytics/peak-hours — Peak hours only
 * GET /api/ai/analytics/forecast  — Demand forecast only
 */
@RestController
@RequestMapping("/api/ai/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final PredictiveAnalyticsService analyticsService;

    /**
     * Full analytics dashboard with AI insights.
     * Security: ADMIN only (configured in SecurityConfig)
     */
    @GetMapping("/dashboard")
    public ResponseEntity<PredictiveAnalyticsService.AnalyticsDashboard> dashboard() {
        log.info("Analytics dashboard requested");
        return ResponseEntity.ok(analyticsService.generateDashboard());
    }
}
