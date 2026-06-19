package com.evcharging.ai.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * RAGController — Phase 5: RAG Question Answering Endpoint
 *
 * POST /api/ai/rag/ask
 * Body: { "question": "What is the FAME-II subsidy for electric cars?" }
 *
 * GET /api/ai/rag/status — Check if knowledge base is loaded
 */
@RestController
@RequestMapping("/api/ai/rag")
@RequiredArgsConstructor
public class RAGController {

    private final RAGQueryService ragQueryService;
    private final RAGDocumentIngestionService ingestionService;

    /**
     * Ask a question answered from the EV knowledge base.
     *
     * Example:
     * POST /api/ai/rag/ask
     * { "question": "What is CCS2 charger?" }
     *
     * Response:
     * {
     *   "answer": "CCS2 is...",
     *   "sourcesUsed": 3,
     *   "retrievedFromKnowledgeBase": true
     * }
     */
    @PostMapping("/ask")
    public ResponseEntity<RAGQueryService.RAGResponse> ask(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(
                RAGQueryService.RAGResponse.builder()
                    .answer("Please provide a 'question' field.")
                    .build()
            );
        }
        return ResponseEntity.ok(ragQueryService.query(question));
    }

    /** Check knowledge base status */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
            "knowledgeBaseReady", ingestionService.isIngestionComplete(),
            "message", ingestionService.isIngestionComplete()
                    ? "EV knowledge base is loaded and ready"
                    : "Knowledge base is still loading..."
        ));
    }
}
