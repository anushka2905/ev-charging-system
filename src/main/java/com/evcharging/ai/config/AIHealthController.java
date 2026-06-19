package com.evcharging.ai.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

/**
 * AIHealthController — Phase 1: Simple AI endpoint to validate LLM integration.
 *
 * WHY this controller:
 *  - Provides a quick sanity-check that Spring AI is wired correctly
 *  - Useful during development to test API key without UI
 *  - Admin-accessible endpoint (secured via Spring Security)
 *
 * Endpoint: GET /api/ai/health?prompt=hello
 * Returns: { "response": "..." , "model": "gpt-4o-mini" }
 */
@RestController
@RequestMapping("/api/ai")
public class AIHealthController {

    private final ChatClient chatClient;

    public AIHealthController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Phase 1 — Basic LLM connectivity test.
     * Secured to ADMIN only (see SecurityConfig).
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck(
            @RequestParam(defaultValue = "Say 'Spring AI is working for EV Charging System!' in one sentence.") String prompt) {

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "prompt", prompt,
                "response", response,
                "model", "gpt-4o-mini"
        ));
    }

    /**
     * Phase 1 — Simple EV question test endpoint.
     * Any authenticated user can call this.
     */
    @GetMapping("/ask")
    public ResponseEntity<Map<String, String>> ask(
            @RequestParam String question) {

        String response = chatClient.prompt()
                .user("Answer this EV-related question concisely: " + question)
                .call()
                .content();

        return ResponseEntity.ok(Map.of(
                "question", question,
                "answer", response
        ));
    }
}
