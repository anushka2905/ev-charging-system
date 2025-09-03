package com.evcharging.service;

import com.evcharging.model.ChargingSlot;
import com.evcharging.repository.ChargingSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChargingSlotServiceImpl implements ChargingSlotService {

    @Autowired
    private ChargingSlotRepository chargingSlotRepository;

    @Override
    public List<ChargingSlot> getAllSlots() {
        return chargingSlotRepository.findAll();
    }

    @Override
    public ChargingSlot getSlotById(Long id) {
        return chargingSlotRepository.findById(id).orElse(null);
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
        ChargingSlot slot = chargingSlotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (!slot.isAvailable()) {
            throw new RuntimeException("Slot is already booked");
        }

        slot.setAvailable(false); // mark slot as booked
        return chargingSlotRepository.save(slot);
    }

	@Autowired
    private ChargingSlotRepository slotRepo;

    @Override
    public List<ChargingSlot> getSlotsByStationId(Long stationId) {
        return slotRepo.findByChargingStationId(stationId);
    }
}
