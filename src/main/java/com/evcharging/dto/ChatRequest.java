package com.evcharging.dto;

import lombok.*;

/**
 * Request body sent from the frontend AI chat widget.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /** The user's message / question */
    private String message;

    /**
     * Optional: conversation session ID.
     * If null, a new conversation is started.
     */
    private String sessionId;
}
