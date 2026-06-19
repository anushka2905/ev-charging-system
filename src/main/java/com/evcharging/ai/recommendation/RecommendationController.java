package com.evcharging.ai.recommendation;

import com.evcharging.dto.StationSummaryDTO;
import com.evcharging.model.ChargingSlot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RecommendationController — Phase 3
 *
 * GET /api/ai/recommend?lat=23.25&lon=77.41&type=DC_FAST&context=long+trip
 *
 * Query Params:
 *   lat     — User latitude  (required)
 *   lon     — User longitude (required)
 *   type    — Charger type (optional): AC_SLOW | AC_FAST | DC_FAST | DC_ULTRA_FAST | CCS2 | CHAdeMO | TYPE2
 *   context — Natural language context (optional)
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

    private final StationRecommendationService recommendationService;

    @GetMapping("/recommend")
    public ResponseEntity<?> recommend(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "Find the best available charging station near me") String context) {

        // Parse charger type (null = no preference)
        ChargingSlot.ChargerType chargerType = null;
        if (type != null && !type.isBlank()) {
            try {
                chargerType = ChargingSlot.ChargerType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid charger type: " + type,
                    "validTypes", "AC_SLOW, AC_FAST, DC_FAST, DC_ULTRA_FAST, CCS2, CHAdeMO, TYPE2"
                ));
            }
        }

        log.info("Recommendation request: lat={} lon={} type={} context={}", lat, lon, type, context);
        List<StationSummaryDTO> recommendations = recommendationService.recommend(lat, lon, chargerType, context);

        return ResponseEntity.ok(Map.of(
            "count", recommendations.size(),
            "recommendations", recommendations
        ));
    }
}
