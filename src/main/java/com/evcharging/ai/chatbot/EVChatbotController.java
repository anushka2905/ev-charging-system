package com.evcharging.ai.chatbot;

import com.evcharging.dto.ChatRequest;
import com.evcharging.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * EVChatbotController — Phase 2: REST API for the EV Assistant Chatbot
 *
 * Endpoints:
 *  POST /api/ai/chat        — Send a message, get AI response
 *  DELETE /api/ai/chat/{id} — Clear conversation session
 *
 * Security:
 *  - Both endpoints require authentication (Spring Security)
 *  - Principal is automatically injected by Spring Security
 *  - User data is isolated per-principal in the service layer
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class EVChatbotController {

    private final EVChatbotService chatbotService;

    /**
     * Main chatbot endpoint.
     *
     * Request Body:
     * {
     *   "message": "Show me available stations in Delhi",
     *   "sessionId": "optional-session-uuid"
     * }
     *
     * Response:
     * {
     *   "reply": "Here are the available stations...",
     *   "sessionId": "uuid",
     *   "detectedIntent": "FIND_STATION",
     *   "confident": true
     * }
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request,
            Principal principal) {

        log.debug("Chat request from user={} sessionId={}",
                principal != null ? principal.getName() : "anonymous",
                request.getSessionId());

        ChatResponse response = chatbotService.chat(request, principal);
        return ResponseEntity.ok(response);
    }

    /**
     * Clear conversation session (user-initiated reset).
     */
    @DeleteMapping("/chat/{sessionId}")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        chatbotService.clearSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
