package com.evcharging.dto;

import lombok.*;

/**
 * Response returned to the frontend AI chat widget.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {

    /** AI-generated reply */
    private String reply;

    /** Echo back the session ID so the client can maintain conversation */
    private String sessionId;

    /** Optional: intent detected by the AI (for Phase 4 NL booking) */
    private String detectedIntent;

    /** true if the AI is confident in its response */
    private boolean confident;
}
