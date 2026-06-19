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
 * Uses Spring AI 1.0.0-M6 compatible APIs.
 * SimpleVectorStore is created via its builder in M6+.
 */
@Configuration
@Slf4j
public class SpringAIConfig {

    @Value("${ev.ai.system-prompt}")
    private String systemPrompt;

    /**
     * PRIMARY ChatClient pre-configured with the EV Assistant system prompt.
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
     * In-memory VectorStore for Phase 5 RAG.
     * Spring AI M6 uses SimpleVectorStore.builder(embeddingModel).build()
     *
     * Production upgrade: replace with PgVectorStore or ChromaVectorStore.
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        log.info("Initializing SimpleVectorStore for RAG (Phase 5)");
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
