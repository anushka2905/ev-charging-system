package com.evcharging.repository;

import com.evcharging.model.ChargingSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargingSlotRepository extends JpaRepository<ChargingSlot, Long> {
    
    // ✅ This works now because 'available' is the correct field name
    List<ChargingSlot> findByAvailableTrue();

    // Optional: more methods
    List<ChargingSlot> findByBookedFalse();
    List<ChargingSlot> findByChargingStation_Id(Long stationId);
	List<ChargingSlot> findByChargingStationId(Long stationId);
	
}
