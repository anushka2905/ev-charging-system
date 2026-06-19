package com.evcharging.ai.chatbot;

import com.evcharging.dto.ChatRequest;
import com.evcharging.dto.ChatResponse;
import com.evcharging.exception.AIServiceException;
import com.evcharging.model.Booking;
import com.evcharging.model.ChargingStation;
import com.evcharging.repository.BookingRepository;
import com.evcharging.repository.ChargingStationRepository;
import com.evcharging.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * EVChatbotService — Phase 2: EV Assistant Chatbot
 *
 * Architecture:
 * ─────────────
 * This service implements a multi-turn conversation assistant with:
 *
 * 1. CONVERSATION MEMORY (In-Memory):
 *    - ConcurrentHashMap<sessionId, List<Message>> holds per-session history
 *    - Each call appends user + assistant messages
 *    - History is bounded to last 10 exchanges (20 messages)
 *    - Production upgrade: replace with Redis or DB-backed store
 *
 * 2. CONTEXT INJECTION:
 *    - Live station data is fetched from DB and injected into the prompt
 *    - User booking history is fetched securely per authenticated user
 *    - This avoids hardcoded responses — AI reasons over real data
 *
 * 3. INTENT DETECTION:
 *    - The AI is asked to identify the intent in the response preamble
 *    - Supported intents: FIND_STATION, BOOK_SLOT, BOOKING_INFO,
 *      CHARGER_INFO, COST_INFO, GENERAL_EV_INFO
 *
 * 4. SECURITY:
 *    - User-specific data (bookings) only injected when Principal is provided
 *    - No cross-user data leakage
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EVChatbotService {

    private static final int MAX_HISTORY_MESSAGES = 20; // 10 exchanges
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final ChatClient chatClient;
    private final ChargingStationRepository stationRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    /**
     * In-memory conversation store.
     * Key: sessionId (UUID)
     * Value: ordered list of chat messages (User + Assistant alternating)
     *
     * Production note: Replace with Redis:
     *   @Autowired RedisTemplate<String, List<Message>> redisTemplate;
     */
    private final Map<String, List<Message>> conversationStore = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────
    //  PUBLIC API
    // ─────────────────────────────────────────────────────────────

    /**
     * Process a user chat message and return an AI response.
     *
     * @param request  contains message + sessionId
     * @param principal Spring Security principal (nullable for public queries)
     */
    public ChatResponse chat(ChatRequest request, Principal principal) {
        String sessionId = resolveSessionId(request.getSessionId());
        List<Message> history = conversationStore.computeIfAbsent(sessionId, k -> new ArrayList<>());

        try {
            // 1. Build context from real DB data
            String contextBlock = buildContextBlock(request.getMessage(), principal);

            // 2. Build the full prompt (system context + history + user message)
            List<Message> messages = buildMessages(contextBlock, history, request.getMessage());

            // 3. Call LLM
            Prompt prompt = new Prompt(messages);
            String rawResponse = chatClient.prompt(prompt).call().content();

            // 4. Parse intent from response
            String intent = detectIntent(request.getMessage());
            String cleanedResponse = rawResponse;

            // 5. Update conversation history (bounded)
            history.add(new UserMessage(request.getMessage()));
            history.add(new AssistantMessage(cleanedResponse));
            trimHistory(history);

            log.info("Chatbot session={} intent={} user={}",
                    sessionId, intent, principal != null ? principal.getName() : "anonymous");

            return ChatResponse.builder()
                    .reply(cleanedResponse)
                    .sessionId(sessionId)
                    .detectedIntent(intent)
                    .confident(true)
                    .build();

        } catch (Exception e) {
            log.error("Chatbot error for session {}: {}", sessionId, e.getMessage(), e);
            throw new AIServiceException("Chatbot service error: " + e.getMessage(), e);
        }
    }

    /** Clear conversation history for a session */
    public void clearSession(String sessionId) {
        conversationStore.remove(sessionId);
    }

    // ─────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────

    /**
     * Builds a context block injected as a system message.
     * This is the KEY to avoiding hardcoded responses — the AI gets
     * fresh data on every turn.
     */
    private String buildContextBlock(String userMessage, Principal principal) {
        StringBuilder ctx = new StringBuilder();

        // ── Live station data ──────────────────────────────────────
        List<ChargingStation> stations = stationRepository.findAll();
        ctx.append("=== AVAILABLE CHARGING STATIONS (Live Data) ===\n");
        if (stations.isEmpty()) {
            ctx.append("No stations currently in the system.\n");
        } else {
            stations.stream().limit(15).forEach(s -> {
                int available = 0;
                try {
                    available = stationRepository.countAvailableSlots(s.getId());
                } catch (Exception ignored) {}
                ctx.append(String.format(
                    "• %s | City: %s | State: %s | Chargers: %s | Available Slots: %d | Price: ₹%.1f/unit | Status: %s\n",
                    s.getName(),
                    s.getCity() != null ? s.getCity() : "N/A",
                    s.getState() != null ? s.getState() : "N/A",
                    s.getChargerTypes() != null ? s.getChargerTypes() : "AC",
                    available,
                    s.getPricePerUnit(),
                    s.getOperationalStatus() != null ? s.getOperationalStatus().name() : "ACTIVE"
                ));
            });
            if (stations.size() > 15) {
                ctx.append(String.format("  ... and %d more stations.\n", stations.size() - 15));
            }
        }

        // ── User booking history (only if authenticated) ───────────
        if (principal != null) {
            ctx.append("\n=== USER BOOKING HISTORY (").append(principal.getName()).append(") ===\n");
            try {
                List<Booking> bookings = bookingRepository.findRecentByUsername(
                        principal.getName(), PageRequest.of(0, 5));
                if (bookings.isEmpty()) {
                    ctx.append("No bookings found for this user.\n");
                } else {
                    bookings.forEach(b -> ctx.append(String.format(
                        "• Booking #%d | Station: %s | Slot: %s | Time: %s | Status: %s | Est. Cost: ₹%.1f\n",
                        b.getId(),
                        b.getSlot() != null && b.getSlot().getChargingStation() != null
                                ? b.getSlot().getChargingStation().getName() : "N/A",
                        b.getSlot() != null ? b.getSlot().getSlotName() : "N/A",
                        b.getBookingTime() != null ? b.getBookingTime().format(FORMATTER) : "N/A",
                        b.getStatus(),
                        b.getEstimatedCost() != null ? b.getEstimatedCost() : 0.0
                    )));
                }
            } catch (Exception e) {
                ctx.append("Could not load booking history.\n");
                log.warn("Failed to load booking history for chatbot: {}", e.getMessage());
            }
        }

        // ── Charging type guide ────────────────────────────────────
        ctx.append("""
            
            === EV CHARGING TYPE GUIDE ===
            • AC Slow (3.3–7.2 kW): Best for overnight charging at home/workplace. ~4-8 hours for full charge.
            • AC Fast (11–22 kW): Public parking lots. ~1-3 hours for full charge.
            • DC Fast (50 kW): Highway stations. ~30-45 minutes for 80% charge.
            • DC Ultra Fast (100–350 kW): Premium stations. ~15-20 minutes for 80% charge.
            • CCS2: Combined Charging Standard — used by most European & Indian EVs (Tata, MG, Hyundai).
            • CHAdeMO: Japanese standard — used by Nissan Leaf.
            • Type-2: European AC standard.
            
            === BOOKING PROCESS ===
            1. Browse stations on the map or list view.
            2. Select a station → View available slots.
            3. Choose a slot → Confirm booking.
            4. Receive booking confirmation.
            5. Complete payment after charging.
            
            === PRICING ===
            Typical range: ₹8–₹25 per unit (kWh) depending on station operator and charger type.
            """);

        return ctx.toString();
    }

    /** Build the complete message list for the LLM call */
    private List<Message> buildMessages(String contextBlock, List<Message> history, String userMessage) {
        List<Message> messages = new ArrayList<>();

        // Context as system message (injected before history)
        messages.add(new SystemMessage(contextBlock));

        // Conversation history (bounded)
        messages.addAll(history);

        // Current user message
        messages.add(new UserMessage(userMessage));

        return messages;
    }

    /** Detect user intent using a keyword heuristic (fast, no extra LLM call) */
    private String detectIntent(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("book") || lower.contains("reserve") || lower.contains("schedule")) {
            return "BOOK_SLOT";
        } else if (lower.contains("station") || lower.contains("find") || lower.contains("near") || lower.contains("location")) {
            return "FIND_STATION";
        } else if (lower.contains("my booking") || lower.contains("my reservation") || lower.contains("history")) {
            return "BOOKING_INFO";
        } else if (lower.contains("charger") || lower.contains("type") || lower.contains("ccs") || lower.contains("ac") || lower.contains("dc")) {
            return "CHARGER_INFO";
        } else if (lower.contains("cost") || lower.contains("price") || lower.contains("rate") || lower.contains("pay")) {
            return "COST_INFO";
        } else if (lower.contains("cancel")) {
            return "CANCEL_BOOKING";
        } else {
            return "GENERAL_EV_INFO";
        }
    }

    /** Keep history bounded to prevent token overflow */
    private void trimHistory(List<Message> history) {
        while (history.size() > MAX_HISTORY_MESSAGES) {
            history.remove(0);
        }
    }

    private String resolveSessionId(String provided) {
        return (provided != null && !provided.isBlank())
                ? provided
                : UUID.randomUUID().toString();
    }
}
