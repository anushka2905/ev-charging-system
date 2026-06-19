package com.evcharging.security;

import com.evcharging.model.ChargingSlot;
import com.evcharging.model.ChargingStation;
import com.evcharging.repository.ChargingSlotRepository;
import com.evcharging.repository.ChargingStationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SlotSeeder — Phase 0 updated.
 * Creates slots with the new ChargerType enum and SlotStatus enum.
 * Backward-compatible with existing DB data.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlotSeeder implements CommandLineRunner {

    private final ChargingStationRepository stationRepository;
    private final ChargingSlotRepository slotRepository;

    // Charger type rotation for demo diversity
    private static final ChargingSlot.ChargerType[] CHARGER_TYPES = {
        ChargingSlot.ChargerType.AC_SLOW,
        ChargingSlot.ChargerType.AC_FAST,
        ChargingSlot.ChargerType.DC_FAST,
        ChargingSlot.ChargerType.AC_SLOW,
        ChargingSlot.ChargerType.CCS2
    };

    private static final double[] POWER_KW = { 3.3, 7.2, 50.0, 7.2, 50.0 };

    @Override
    public void run(String... args) {
        if (slotRepository.count() == 0) {
            List<ChargingStation> stations = stationRepository.findAll();

            for (ChargingStation station : stations) {
                for (int i = 1; i <= 5; i++) {
                    int typeIdx = (i - 1) % CHARGER_TYPES.length;

                    ChargingSlot slot = ChargingSlot.builder()
                            .slotName("Slot-" + i)
                            .connectorId("CON-" + station.getId() + "-" + i)
                            .status(ChargingSlot.SlotStatus.AVAILABLE)
                            .chargerType(CHARGER_TYPES[typeIdx])
                            .powerKw(POWER_KW[typeIdx])
                            .chargingStation(station)
                            .build();

                    slotRepository.save(slot);
                }
            }
            log.info("✅ Slots seeded for {} stations", stations.size());
        } else {
            log.info("Slots already exist. Skipping seeder.");
        }
    }
}
