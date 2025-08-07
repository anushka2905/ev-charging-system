package com.evcharging.security;

import com.evcharging.model.ChargingSlot;
import com.evcharging.model.ChargingStation;
import com.evcharging.repository.ChargingSlotRepository;
import com.evcharging.repository.ChargingStationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

@Component
public class SlotSeeder implements CommandLineRunner {

    private final ChargingStationRepository stationRepository;
    private final ChargingSlotRepository slotRepository;

    public SlotSeeder(ChargingStationRepository stationRepository, ChargingSlotRepository slotRepository) {
        this.stationRepository = stationRepository;
        this.slotRepository = slotRepository;
    }

    @Override
    public void run(String... args) {
        if (slotRepository.count() == 0) {
            List<ChargingStation> stations = stationRepository.findAll();

            for (ChargingStation station : stations) {
                LocalTime start = LocalTime.of(9, 0); // Start at 9:00 AM

                for (int i = 1; i <= 5; i++) {
                    LocalTime end = start.plusHours(1);

                    ChargingSlot slot = new ChargingSlot();
                    slot.setSlotName("Slot " + i);
                    slot.setAvailable(true);
                    slot.setBooked(false);
                    slot.setChargingStation(station); // set station relation

                    // Optional: If you have start and end time fields, set them here
                    // slot.setStartTime(start);
                    // slot.setEndTime(end);

                    slotRepository.save(slot);
                    start = end; // move to next slot
                }
            }
            System.out.println("✅ Time-based slots seeded successfully!");
        } else {
            System.out.println("⚠ Slots already exist. Skipping seeding.");
        }
    }
}
