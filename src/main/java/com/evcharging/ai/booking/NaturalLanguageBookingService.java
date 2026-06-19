package com.evcharging.ai.booking;

import com.evcharging.dto.NLBookingRequest;
import com.evcharging.dto.NLBookingResponse;
import com.evcharging.exception.AIServiceException;
import com.evcharging.exception.SlotAlreadyBookedException;
import com.evcharging.model.Booking;
import com.evcharging.model.ChargingSlot;
import com.evcharging.model.ChargingStation;
import com.evcharging.model.User;
import com.evcharging.repository.BookingRepository;
import com.evcharging.repository.ChargingSlotRepository;
import com.evcharging.repository.ChargingStationRepository;
import com.evcharging.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * NaturalLanguageBookingService — Phase 4
 *
 * Architecture:
 * ─────────────
 * This service converts free-text booking requests into structured booking actions.
 *
 * Pipeline:
 * ┌──────────────────────────────────────────────────────────────┐
 * │ 1. USER INPUT:  "Book a DC fast charger tomorrow at 6 PM    │
 * │                  at Green Energy Station"                    │
 * │                                                              │
 * │ 2. AI PARSING:  LLM extracts structured JSON:               │
 * │    {                                                         │
 * │      "stationName": "Green Energy Station",                  │
 * │      "preferredChargerType": "DC_FAST",                      │
 * │      "requestedStartTime": "2024-01-16T18:00:00",           │
 * │      "durationHours": 1.0                                    │
 * │    }                                                         │
 * │                                                              │
 * │ 3. VALIDATION:  Check slot availability in DB               │
 * │                                                              │
 * │ 4. BOOKING:     Create booking record                       │
 * │                                                              │
 * │ 5. RESPONSE:    Confirmation with booking details            │
 * └──────────────────────────────────────────────────────────────┘
 *
 * Why this approach?
 *  - AI handles the complexity of understanding date/time expressions
 *    ("tomorrow evening", "next Friday", "in 2 hours")
 *  - Business validation remains in Java (safe, testable, reliable)
 *  - Errors are caught gracefully before any DB write
 *  - The nlQuery field stores the original text for audit/replay
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NaturalLanguageBookingService {

    private final ChatClient chatClient;
    private final ChargingStationRepository stationRepository;
    private final ChargingSlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ─────────────────────────────────────────────────────────────
    //  PUBLIC API
    // ─────────────────────────────────────────────────────────────

    /**
     * Process a natural language booking request.
     *
     * @param nlText   Free-text booking request from the user
     * @param username Authenticated user's username
     * @return NLBookingResponse with status and booking ID (if successful)
     */
    @Transactional
    public NLBookingResponse processBookingRequest(String nlText, String username) {
        log.info("NL Booking request from user={}: '{}'", username, nlText);

        try {
            // Step 1: Parse the natural language input into structured request
            NLBookingRequest parsed = parseWithAI(nlText);
            parsed.setNlText(nlText);

            // Step 2: Validate and resolve station + slot
            NLBookingRequest resolved = resolveStationAndSlot(parsed);
            if (resolved.getResolvedSlotId() == null) {
                return NLBookingResponse.builder()
                        .status(NLBookingResponse.NLBookingStatus.SLOT_UNAVAILABLE)
                        .errorMessage("No available slot found matching: charger type="
                                + parsed.getPreferredChargerType()
                                + " at station=" + parsed.getStationName())
                        .parsedRequest(parsed)
                        .build();
            }

            // Step 3: Create the booking
            Booking booking = createBooking(resolved, username);

            // Step 4: Build confirmation message
            String confirmation = buildConfirmationMessage(booking);

            return NLBookingResponse.builder()
                    .status(NLBookingResponse.NLBookingStatus.SUCCESS)
                    .bookingId(booking.getId())
                    .confirmationMessage(confirmation)
                    .parsedRequest(resolved)
                    .build();

        } catch (SlotAlreadyBookedException e) {
            return NLBookingResponse.builder()
                    .status(NLBookingResponse.NLBookingStatus.SLOT_UNAVAILABLE)
                    .errorMessage(e.getMessage())
                    .build();

        } catch (AIServiceException e) {
            throw e;

        } catch (Exception e) {
            log.error("NL Booking failed for user={}: {}", username, e.getMessage(), e);
            return NLBookingResponse.builder()
                    .status(NLBookingResponse.NLBookingStatus.PARSE_ERROR)
                    .errorMessage("Could not process your request: " + e.getMessage())
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  STEP 1: AI Parsing
    // ─────────────────────────────────────────────────────────────

    /**
     * Uses the LLM to parse natural language into a structured NLBookingRequest.
     * The prompt instructs the AI to output VALID JSON only.
     */
    private NLBookingRequest parseWithAI(String nlText) {
        LocalDateTime now = LocalDateTime.now();

        // Build current station names for context
        List<ChargingStation> stations = stationRepository.findAll();
        StringBuilder stationNames = new StringBuilder();
        stations.stream().limit(20).forEach(s ->
            stationNames.append("- ").append(s.getName())
                        .append(" (").append(s.getCity()).append(")\n")
        );

        String parsePrompt = String.format("""
            Today's date and time: %s
            
            Available charging stations in our system:
            %s
            
            Valid charger types: AC_SLOW, AC_FAST, DC_FAST, DC_ULTRA_FAST, CCS2, CHAdeMO, TYPE2
            
            Parse this booking request into JSON:
            "%s"
            
            Output ONLY valid JSON in this exact format (no explanation, no markdown):
            {
              "stationName": "station name or null if not mentioned",
              "city": "city name or null",
              "preferredChargerType": "DC_FAST or null if not specified",
              "requestedStartTime": "ISO datetime like 2024-01-16T18:00:00 or null",
              "durationHours": 1.0
            }
            
            Rules:
            - If user says "tomorrow", compute the actual date from today
            - If user says "this evening", use 18:00 today
            - If no duration mentioned, default to 1.0 hour
            - If charger type not specified, use null
            - If station name not mentioned, use null
            - Output ONLY the JSON object, nothing else
            """,
            now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
            stationNames.toString(),
            nlText
        );

        try {
            String jsonResponse = chatClient.prompt()
                    .system("You are a JSON parser. Output ONLY valid JSON, no explanations.")
                    .user(parsePrompt)
                    .call()
                    .content();

            // Clean the response (remove any markdown code blocks if present)
            String cleaned = jsonResponse.trim()
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            log.debug("AI parsed NL booking: {}", cleaned);
            return objectMapper.readValue(cleaned, NLBookingRequest.class);

        } catch (Exception e) {
            log.error("AI parsing failed: {}", e.getMessage());
            throw new AIServiceException("Failed to parse booking request: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  STEP 2: Validation & Resolution
    // ─────────────────────────────────────────────────────────────

    /**
     * Resolves the parsed request to actual DB entities.
     * Sets resolvedStationId and resolvedSlotId.
     */
    private NLBookingRequest resolveStationAndSlot(NLBookingRequest parsed) {
        // Find matching station
        List<ChargingStation> matchingStations;
        if (parsed.getStationName() != null && !parsed.getStationName().equalsIgnoreCase("null")) {
            matchingStations = stationRepository.searchByAny(parsed.getStationName());
        } else if (parsed.getCity() != null && !parsed.getCity().equalsIgnoreCase("null")) {
            matchingStations = stationRepository.findByCityContainingIgnoreCase(parsed.getCity());
        } else {
            matchingStations = stationRepository.findAll();
        }

        if (matchingStations.isEmpty()) {
            log.warn("No station found for name='{}' city='{}'", parsed.getStationName(), parsed.getCity());
            return parsed;
        }

        // Find the first station with an available slot of the preferred type
        for (ChargingStation station : matchingStations) {
            List<ChargingSlot> availableSlots;

            if (parsed.getPreferredChargerType() != null) {
                availableSlots = slotRepository.findAvailableByStationAndType(
                        station.getId(), parsed.getPreferredChargerType());
            } else {
                availableSlots = slotRepository.findByChargingStation_IdAndStatus(
                        station.getId(), ChargingSlot.SlotStatus.AVAILABLE);
            }

            if (!availableSlots.isEmpty()) {
                ChargingSlot slot = availableSlots.get(0);
                parsed.setResolvedStationId(station.getId());
                parsed.setResolvedSlotId(slot.getId());
                log.info("Resolved: station={} slot={}", station.getName(), slot.getSlotName());
                return parsed;
            }
        }

        log.warn("No available slot found for the given criteria");
        return parsed;
    }

    // ─────────────────────────────────────────────────────────────
    //  STEP 3: Create Booking
    // ─────────────────────────────────────────────────────────────

    private Booking createBooking(NLBookingRequest resolved, String username) {
        ChargingSlot slot = slotRepository.findById(resolved.getResolvedSlotId())
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (slot.isBooked()) {
            throw new SlotAlreadyBookedException(slot.getId());
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Parse start time
        LocalDateTime startTime = null;
        if (resolved.getRequestedStartTime() != null
                && !resolved.getRequestedStartTime().equalsIgnoreCase("null")) {
            try {
                startTime = LocalDateTime.parse(resolved.getRequestedStartTime(), ISO_FORMAT);
            } catch (Exception e) {
                log.warn("Could not parse start time '{}', using now", resolved.getRequestedStartTime());
                startTime = LocalDateTime.now().plusHours(1);
            }
        } else {
            startTime = LocalDateTime.now().plusHours(1);
        }

        double duration = resolved.getDurationHours() > 0 ? resolved.getDurationHours() : 1.0;
        LocalDateTime endTime = startTime.plusMinutes((long)(duration * 60));

        // Estimate cost
        double estimatedCost = slot.getPowerKw() * duration
                * slot.getChargingStation().getPricePerUnit();

        Booking booking = Booking.builder()
                .slot(slot)
                .user(user)
                .bookingTime(LocalDateTime.now())
                .scheduledStartTime(startTime)
                .scheduledEndTime(endTime)
                .durationHours(duration)
                .estimatedCost(estimatedCost)
                .status(Booking.Status.BOOKED)
                .nlQuery(resolved.getNlText())
                .build();

        // Mark slot as booked
        slot.setStatus(ChargingSlot.SlotStatus.BOOKED);
        slot.setLastBookedAt(LocalDateTime.now());
        slotRepository.save(slot);

        return bookingRepository.save(booking);
    }

    // ─────────────────────────────────────────────────────────────
    //  STEP 4: Confirmation Message
    // ─────────────────────────────────────────────────────────────

    private String buildConfirmationMessage(Booking booking) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy 'at' hh:mm a");
        String stationName = booking.getSlot().getChargingStation().getName();
        String slotName = booking.getSlot().getSlotName();
        String chargerType = booking.getSlot().getChargerType() != null
                ? booking.getSlot().getChargerType().getDisplayName() : "Standard";

        return String.format(
            "✅ Booking Confirmed!\n" +
            "📍 Station: %s\n" +
            "🔌 Slot: %s (%s)\n" +
            "📅 Start: %s\n" +
            "⏱️ Duration: %.1f hour(s)\n" +
            "💰 Estimated Cost: ₹%.2f\n" +
            "🆔 Booking ID: #%d",
            stationName, slotName, chargerType,
            booking.getScheduledStartTime() != null
                    ? booking.getScheduledStartTime().format(fmt) : "Scheduled",
            booking.getDurationHours(),
            booking.getEstimatedCost() != null ? booking.getEstimatedCost() : 0.0,
            booking.getId()
        );
    }
}
