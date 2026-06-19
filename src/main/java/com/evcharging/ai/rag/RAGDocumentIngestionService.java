package com.evcharging.ai.rag;

import com.evcharging.exception.AIServiceException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * RAGDocumentIngestionService — Phase 5: Document Ingestion Pipeline
 *
 * Architecture:
 * ─────────────
 * RAG (Retrieval-Augmented Generation) works in 2 phases:
 *
 * PHASE A — Ingestion (this service, runs at startup):
 * ┌─────────────────────────────────────────────────────────────┐
 * │  Raw Documents (PDF/TXT/HTML)                               │
 * │       ↓                                                     │
 * │  Document Reader (TikaDocumentReader)                       │
 * │       ↓                                                     │
 * │  Text Splitter (TokenTextSplitter, chunk_size=800)          │
 * │       ↓                                                     │
 * │  Embedding Model (text-embedding-3-small → 1536 dim vector) │
 * │       ↓                                                     │
 * │  VectorStore (SimpleVectorStore in-memory)                  │
 * └─────────────────────────────────────────────────────────────┘
 *
 * PHASE B — Retrieval+Generation (RAGQueryService):
 * ┌─────────────────────────────────────────────────────────────┐
 * │  User Query                                                 │
 * │       ↓                                                     │
 * │  Embed query → similarity search in VectorStore             │
 * │       ↓                                                     │
 * │  Top-K relevant document chunks retrieved                   │
 * │       ↓                                                     │
 * │  Inject chunks as context into LLM prompt                  │
 * │       ↓                                                     │
 * │  LLM generates grounded answer                              │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Documents loaded from: src/main/resources/documents/
 * Supported formats: .txt, .pdf, .html, .docx
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RAGDocumentIngestionService {

    private final VectorStore vectorStore;

    @Value("${ev.rag.chunk-size:800}")
    private int chunkSize;

    @Value("${ev.rag.chunk-overlap:100}")
    private int chunkOverlap;

    private boolean ingestionComplete = false;

    /**
     * Automatically ingest documents on application startup.
     * Uses @PostConstruct to run after Spring context is ready.
     */
    @PostConstruct
    public void ingestDocuments() {
        log.info("=== RAG Phase 5: Starting document ingestion ===");

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources;

            try {
                resources = resolver.getResources("classpath:documents/**");
            } catch (Exception e) {
                log.warn("No documents found in classpath:documents/ — RAG will use empty knowledge base");
                ingestDefaultKnowledge();
                return;
            }

            if (resources.length == 0) {
                log.warn("documents/ directory is empty — loading default EV knowledge");
                ingestDefaultKnowledge();
                return;
            }

            List<Document> allDocuments = new ArrayList<>();
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(chunkSize)
                    .withMinChunkSizeChars(100)
                    .withKeepSeparator(true)
                    .build();

            Arrays.stream(resources)
                    .filter(r -> {
                        try {
                            String fname = r.getFilename();
                            return fname != null && (
                                    fname.endsWith(".txt") ||
                                    fname.endsWith(".pdf") ||
                                    fname.endsWith(".html") ||
                                    fname.endsWith(".docx"));
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .forEach(resource -> {
                        try {
                            log.info("Ingesting document: {}", resource.getFilename());
                            TikaDocumentReader reader = new TikaDocumentReader(resource);
                            List<Document> docs = reader.get();
                            List<Document> chunks = splitter.apply(docs);
                            allDocuments.addAll(chunks);
                            log.info("  → {} chunks from {}", chunks.size(), resource.getFilename());
                        } catch (Exception e) {
                            log.warn("Failed to ingest {}: {}", resource.getFilename(), e.getMessage());
                        }
                    });

            if (!allDocuments.isEmpty()) {
                vectorStore.add(allDocuments);
                log.info("=== RAG ingestion complete: {} chunks added to vector store ===", allDocuments.size());
                ingestionComplete = true;
            } else {
                log.warn("No documents were successfully ingested — loading default knowledge");
                ingestDefaultKnowledge();
            }

        } catch (Exception e) {
            log.error("RAG ingestion failed: {} — loading default knowledge", e.getMessage(), e);
            ingestDefaultKnowledge();
        }
    }

    /**
     * Load built-in EV knowledge when no external documents are provided.
     * This ensures RAG works out-of-the-box without requiring document files.
     *
     * Production: Replace this with actual PDF files in src/main/resources/documents/
     */
    private void ingestDefaultKnowledge() {
        log.info("Loading default EV knowledge base into vector store...");

        List<String> knowledgeChunks = List.of(
            // Charging Standards
            "EV Charging Standards Guide: " +
            "CCS2 (Combined Charging System 2) is the most widely adopted standard in India and Europe. " +
            "It supports both AC and DC charging. Most modern EVs from Tata Motors (Nexon EV, Tigor EV), " +
            "MG Motor (ZS EV), and Hyundai (Kona EV, Ioniq 5) use CCS2. " +
            "CHAdeMO is a DC fast charging standard originally from Japan, used by Nissan Leaf. " +
            "Type-2 (IEC 62196) is a European AC charging standard for Level 2 charging. " +
            "Bharat AC-001 and Bharat DC-001 are India-specific standards for 3.3kW and 15kW respectively.",

            // Charging Speeds
            "EV Charging Speed Guide: " +
            "Level 1 (AC Slow, 3.3 kW): Uses a standard household outlet. Adds ~15-20 km of range per hour. " +
            "Best for overnight home charging. Full charge takes 8-12 hours. " +
            "Level 2 (AC Fast, 7.2-22 kW): Requires dedicated charging equipment. Adds ~40-100 km per hour. " +
            "Typical for workplace and public charging. Full charge takes 2-4 hours. " +
            "DC Fast Charging (50 kW): Charges to 80% in 30-45 minutes. Found at highway corridors. " +
            "DC Ultra Fast (100-350 kW): Charges to 80% in 15-20 minutes. Premium stations. " +
            "Note: Always charge to 80% for regular use to preserve battery health.",

            // Government EV Policy India
            "India EV Policy Overview (FAME-II): " +
            "The Faster Adoption and Manufacturing of Electric Vehicles (FAME) India Phase II scheme " +
            "provides subsidies up to ₹1.5 lakh for electric 4-wheelers and ₹10,000 for 2-wheelers. " +
            "The Bureau of Energy Efficiency (BEE) mandates at least one public charging station every " +
            "3 km in cities and every 25 km on highways. 30% GST reduction on EV purchases. " +
            "State governments offer additional subsidies: Maharashtra (up to ₹2.5 lakh), " +
            "Delhi (up to ₹1.5 lakh), Gujarat (up to ₹1.5 lakh). " +
            "Production Linked Incentive (PLI) scheme supports domestic EV manufacturing.",

            // EV Battery Guide
            "EV Battery Technology Guide: " +
            "Modern EVs use Lithium-Ion (Li-Ion) or Lithium Iron Phosphate (LFP) batteries. " +
            "LFP batteries (used in Tata Nexon EV Max) have longer cycle life and better thermal safety " +
            "but slightly lower energy density. NMC (Nickel Manganese Cobalt) offers higher energy density. " +
            "Typical warranty: 8 years or 160,000 km (whichever comes first). " +
            "Temperature affects range: expect 15-25% range reduction in extreme cold (below 0°C) or heat. " +
            "Optimal charging temperature: 15°C to 35°C. " +
            "Fast charging generates heat — battery management systems (BMS) limit charging rate when hot.",

            // Booking & Payment Guide
            "EV Charging Station Booking Guide: " +
            "To book a charging slot: Log in to your account → Browse stations on the map or list → " +
            "Select a station → Choose an available slot → Confirm your booking → " +
            "Arrive at the station and start charging. " +
            "Payment methods accepted: UPI (GPay, PhonePe, Paytm), Credit/Debit Card, " +
            "Net Banking, Digital Wallets. " +
            "Cancellation policy: Cancel at least 1 hour before the scheduled time for a full refund. " +
            "Late cancellations may incur a ₹50 fee. " +
            "Cost calculation: Price per kWh × Energy consumed (kWh). " +
            "Average cost: ₹8-₹15/kWh for AC charging, ₹18-₹25/kWh for DC fast charging.",

            // EV Maintenance
            "EV Maintenance Tips: " +
            "EVs have fewer moving parts than ICE vehicles — no oil changes, spark plugs, or exhaust. " +
            "Regular maintenance includes: tyre rotation (every 10,000 km), brake fluid check (every 2 years), " +
            "cabin air filter replacement (annually), coolant check for battery thermal system. " +
            "Software updates: Keep your vehicle's software updated for performance improvements. " +
            "Regenerative braking reduces brake pad wear significantly — brake pads last 2-3x longer. " +
            "Drive in Eco mode for maximum range. Use scheduled charging to charge during off-peak hours.",

            // Troubleshooting
            "EV Charging Troubleshooting FAQ: " +
            "Q: Charger not working? A: Check if the station is ACTIVE. Try a different connector. " +
            "Contact station operator if the issue persists. " +
            "Q: Charging too slow? A: Ensure your vehicle supports the charger's power level. " +
            "Battery preconditioning helps for DC fast charging. " +
            "Q: Lost charging card/app access? A: Contact support immediately to suspend your account. " +
            "Q: Can I charge in rain? A: Yes, EV charging equipment is rated IP54 or higher — waterproof. " +
            "Q: What is range anxiety? A: Fear of running out of charge. Plan routes using EV charging apps. " +
            "India has 6,000+ public charging points as of 2024, growing rapidly."
        );

        List<Document> documents = knowledgeChunks.stream()
                .map(content -> new Document(content))
                .toList();

        vectorStore.add(documents);
        ingestionComplete = true;
        log.info("Default EV knowledge base loaded: {} documents", documents.size());
    }

    public boolean isIngestionComplete() {
        return ingestionComplete;
    }
}
