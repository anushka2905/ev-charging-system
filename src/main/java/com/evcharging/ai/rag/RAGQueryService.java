package com.evcharging.ai.rag;

import com.evcharging.exception.AIServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAGQueryService — Phase 5: Retrieval-Augmented Generation Query
 *
 * Updated for Spring AI 1.0.0-M6 API:
 *  - SearchRequest uses SearchRequest.builder() instead of SearchRequest.query()
 *  - Document.getText() replaces Document.getContent()
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RAGQueryService {

    private final ChatClient chatClient;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RAGDocumentIngestionService ingestionService;

    @Value("${ev.rag.top-k:5}")
    private int topK;

    @Value("${ev.rag.similarity-threshold:0.7}")
    private double similarityThreshold;

    // ─────────────────────────────────────────────────────────────
    //  PUBLIC API
    // ─────────────────────────────────────────────────────────────

    /**
     * Answer a question using RAG over the EV knowledge base.
     */
    public RAGResponse query(String question) {
        log.info("RAG query: '{}'", question);

        try {
            List<Document> relevantDocs = retrieveRelevantDocs(question);

            if (relevantDocs.isEmpty()) {
                log.warn("No relevant documents found for: '{}' — falling back to LLM knowledge", question);
                return fallbackToLLM(question);
            }

            String context = buildContext(relevantDocs);
            String answer = generateAnswer(question, context);

            return RAGResponse.builder()
                    .answer(answer)
                    .sourcesUsed(relevantDocs.size())
                    .retrievedFromKnowledgeBase(true)
                    .build();

        } catch (Exception e) {
            log.error("RAG query failed: {}", e.getMessage(), e);
            throw new AIServiceException("RAG query failed: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  STEP 1: Retrieval  (Spring AI M6 API)
    // ─────────────────────────────────────────────────────────────

    private List<Document> retrieveRelevantDocs(String question) {
        // Spring AI M6: use SearchRequest.builder() instead of SearchRequest.query()
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();

        List<Document> docs = vectorStoreProvider.getObject().similaritySearch(searchRequest);
        log.debug("Retrieved {} relevant documents for query: '{}'", docs.size(), question);
        return docs;
    }

    // ─────────────────────────────────────────────────────────────
    //  STEP 2: Context Building  (Spring AI M6: getText() not getContent())
    // ─────────────────────────────────────────────────────────────

    private String buildContext(List<Document> documents) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("=== RELEVANT KNOWLEDGE BASE CONTENT ===\n\n");

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            ctx.append("--- Excerpt ").append(i + 1).append(" ---\n");
            // Spring AI M6: getText() replaces getContent()
            ctx.append(doc.getText()).append("\n\n");
        }

        return ctx.toString();
    }

    // ─────────────────────────────────────────────────────────────
    //  STEP 3: Generation
    // ─────────────────────────────────────────────────────────────

    private String generateAnswer(String question, String context) {
        String ragPrompt = String.format("""
            You are an expert EV (Electric Vehicle) assistant with deep knowledge about
            charging technology, policies, and best practices.

            Use ONLY the information from the knowledge base below to answer the question.
            If the knowledge base doesn't contain enough information to answer, say so clearly
            and provide what you can from general EV knowledge.

            %s

            Question: %s

            Provide a clear, helpful, and accurate answer.
            """,
                context, question
        );

        return chatClient.prompt()
                .system("You are an expert EV assistant. Answer based on the provided context.")
                .user(ragPrompt)
                .call()
                .content();
    }

    // ─────────────────────────────────────────────────────────────
    //  Fallback
    // ─────────────────────────────────────────────────────────────

    private RAGResponse fallbackToLLM(String question) {
        String answer = chatClient.prompt()
                .user("Answer this EV-related question based on your knowledge: " + question)
                .call()
                .content();

        return RAGResponse.builder()
                .answer(answer)
                .sourcesUsed(0)
                .retrievedFromKnowledgeBase(false)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    //  Response Model
    // ─────────────────────────────────────────────────────────────

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RAGResponse {
        private String answer;
        private int sourcesUsed;
        private boolean retrievedFromKnowledgeBase;
        private String disclaimer;
    }
}
