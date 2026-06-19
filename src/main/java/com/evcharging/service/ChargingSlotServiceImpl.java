package com.evcharging.service;

import com.evcharging.exception.ResourceNotFoundException;
import com.evcharging.exception.SlotAlreadyBookedException;
import com.evcharging.model.ChargingSlot;
import com.evcharging.repository.ChargingSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ChargingSlotServiceImpl — Phase 0 refactored.
 * Uses constructor injection, proper exceptions, SlotStatus enum.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChargingSlotServiceImpl implements ChargingSlotService {

    private final ChargingSlotRepository chargingSlotRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChargingSlot> getAllSlots() {
        return chargingSlotRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public ChargingSlot getSlotById(Long id) {
        return chargingSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ChargingSlot", id));
    }

    @Override
    public ChargingSlot saveSlot(ChargingSlot slot) {
        return chargingSlotRepository.save(slot);
    }

    @Override
    public void deleteSlot(Long id) {
        chargingSlotRepository.deleteById(id);
    }

    @Override
    public ChargingSlot bookSlot(Long slotId) {
        ChargingSlot slot = getSlotById(slotId);
        if (!slot.isAvailable()) {
            throw new SlotAlreadyBookedException(slotId);
        }
        slot.setStatus(ChargingSlot.SlotStatus.BOOKED);
        return chargingSlotRepository.save(slot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingSlot> getSlotsByStationId(Long stationId) {
        return chargingSlotRepository.findByChargingStationId(stationId);
    }
}
