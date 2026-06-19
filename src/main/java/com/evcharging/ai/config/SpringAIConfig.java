package com.evcharging.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * SpringAIConfig — Phase 1: Spring AI Integration
 *
 * Architecture Decision:
 * ─────────────────────
 * We use OpenAI GPT-4o-mini as the primary LLM because:
 *  1. Spring AI has first-class OpenAI support with auto-configuration
 *  2. GPT-4o-mini is cost-effective for EV chatbot workloads
 *  3. The same API key works for both Chat and Embeddings (text-embedding-3-small)
 *
 * To switch to Gemini: replace spring-ai-openai-spring-boot-starter with
 * spring-ai-vertex-ai-gemini-spring-boot-starter and update properties.
 * The service layer code remains unchanged (Spring AI abstraction).
 *
 * Beans created here:
 *  - ChatClient (fluent builder for AI calls with system prompt)
 *  - VectorStore (SimpleVectorStore for Phase 5 RAG — swap for PgVector/Chroma in prod)
 */
@Configuration
@Slf4j
public class SpringAIConfig {

    @Value("${ev.ai.system-prompt}")
    private String systemPrompt;

    /**
     * PRIMARY ChatClient — pre-configured with:
     *  - EV Assistant system prompt
     *  - Default model options (from application.properties)
     *
     * Why ChatClient over ChatModel?
     * ChatClient is the high-level fluent API that supports:
     *  - System prompts
     *  - Conversation history (advisors)
     *  - Tool/function calling
     *  - RAG advisors (Phase 5)
     * ChatModel is the lower-level interface.
     */
    @Bean
    @Primary
    public ChatClient chatClient(ChatModel chatModel) {
        log.info("Initializing Spring AI ChatClient with EV system prompt");
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .build();
    }

    /**
     * VectorStore for Phase 5 RAG.
     *
     * SimpleVectorStore = in-memory, no external dependency.
     * Production recommendation: use PgVectorStore (PostgreSQL pgvector)
     * or ChromaVectorStore for persistence and scalability.
     *
     * Replace with:
     *   @Bean PgVectorStore vectorStore(JdbcTemplate jdbc, EmbeddingModel em) { ... }
     * when ready for production.
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        log.info("Initializing SimpleVectorStore for RAG (Phase 5)");
        return new SimpleVectorStore(embeddingModel);
    }
}
