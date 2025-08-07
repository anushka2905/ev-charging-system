// ChargingStationRepository.java
package com.evcharging.repository;

import com.evcharging.model.ChargingStation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChargingStationRepository extends JpaRepository<ChargingStation, Long> {
    List<ChargingStation> findByCity(String city);
    List<ChargingStation> findByCityContainingIgnoreCase(String city);
    List<ChargingStation> findByNameContainingIgnoreCase(String name);
    List<ChargingStation> findByCityContainingIgnoreCaseAndNameContainingIgnoreCase(String city, String name);
	List<ChargingStation> findByNameContainingIgnoreCaseAndCityContainingIgnoreCase(String name, String city);
    
}
