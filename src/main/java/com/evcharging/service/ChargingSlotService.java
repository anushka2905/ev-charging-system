package com.evcharging.service;

import com.evcharging.model.ChargingSlot;

import java.util.List;

public interface ChargingSlotService {
    List<ChargingSlot> getAllSlots();
    ChargingSlot getSlotById(Long id);
    ChargingSlot saveSlot(ChargingSlot slot);
    void deleteSlot(Long id);
	List<ChargingSlot> getSlotsByStationId(Long stationId);
	
}
