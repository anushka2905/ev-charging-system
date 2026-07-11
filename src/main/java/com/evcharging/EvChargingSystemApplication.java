package com.evcharging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI-Powered EV Charging Station Management System
 *
 * Architecture Overview:
 * ─────────────────────
 * Phase 0: Refactored Entities + Multi-city Support
 * Phase 1: Spring AI Integration (OpenAI / Gemini)
 * Phase 2: EV Assistant Chatbot with Conversation Memory
 * Phase 3: Smart Station Recommendation Engine (Hybrid AI)
 * Phase 4: Natural Language Booking (AI → Structured Action)
 * Phase 5: RAG (Retrieval-Augmented Generation) Knowledge Base
 * Phase 6: Predictive Analytics with AI Insights
 */
@SpringBootApplication
@EnableAsync
public class EvChargingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvChargingSystemApplication.class, args);
    }

    /**
     * Configure Jackson ObjectMapper with JavaTimeModule for LocalDateTime.
     * Used by NaturalLanguageBookingService for JSON parsing of AI responses.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
