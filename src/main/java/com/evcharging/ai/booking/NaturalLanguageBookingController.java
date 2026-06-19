package com.evcharging.ai.booking;

import com.evcharging.dto.NLBookingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * NaturalLanguageBookingController — Phase 4
 *
 * POST /api/ai/book
 * Body: { "message": "Book a DC fast charger at Green Station tomorrow at 6 PM" }
 *
 * Security: Requires ROLE_USER or ROLE_ADMIN
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class NaturalLanguageBookingController {

    private final NaturalLanguageBookingService nlBookingService;

    /**
     * Natural language booking endpoint.
     *
     * Example request:
     * POST /api/ai/book
     * {
     *   "message": "Reserve a fast charging slot tonight at 8 PM for 2 hours"
     * }
     */
    @PostMapping("/book")
    public ResponseEntity<NLBookingResponse> bookWithNaturalLanguage(
            @RequestBody Map<String, String> body,
            Principal principal) {

        String message = body.get("message");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(
                NLBookingResponse.builder()
                    .status(NLBookingResponse.NLBookingStatus.VALIDATION_ERROR)
                    .errorMessage("message field is required")
                    .build()
            );
        }

        log.info("NL Booking: user={} message='{}'",
                principal.getName(), message);

        NLBookingResponse response = nlBookingService.processBookingRequest(message, principal.getName());
        return ResponseEntity.ok(response);
    }
}
