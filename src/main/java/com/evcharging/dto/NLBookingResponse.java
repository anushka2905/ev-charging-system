package com.evcharging.dto;

import lombok.*;

/**
 * DTO for the Phase 4 NL Booking response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NLBookingResponse {

    public enum NLBookingStatus {
        SUCCESS,
        SLOT_UNAVAILABLE,
        AMBIGUOUS,
        PARSE_ERROR,
        VALIDATION_ERROR
    }

    private NLBookingStatus status;
    private Long bookingId;
    private String confirmationMessage;
    private String errorMessage;

    /** The parsed request so the user can confirm details */
    private NLBookingRequest parsedRequest;
}
