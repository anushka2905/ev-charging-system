package com.evcharging.service;

import com.evcharging.model.ChargingStation;
import com.evcharging.repository.ChargingStationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChargingStationServiceImpl implements ChargingStationService {

    @Autowired
    private ChargingStationRepository stationRepo;

    @Override
    public ChargingStation saveStation(ChargingStation station) {
        return stationRepo.save(station);
    }

    @Override
    public List<ChargingStation> getAllStations() {
        return stationRepo.findAll();
    }

    @Override
    public ChargingStation getStationById(Long id) {
        return stationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Station not found with ID: " + id));
    }

    @Override
    public void deleteStation(Long id) {
        stationRepo.deleteById(id);
    }

    @Override
    public List<ChargingStation> searchStations(String city, String name) {
        return stationRepo.findByCityContainingIgnoreCaseAndNameContainingIgnoreCase(city, name);
    }

    @Override
    public List<ChargingStation> searchByNameAndCity(String name, String city) {
        return stationRepo.findByNameContainingIgnoreCaseAndCityContainingIgnoreCase(name, city);
    }
    @Autowired
    private ChargingStationRepository stationRepository;
    
	@Override
	public void deleteStationById(Long id) {
		 stationRepository.deleteById(id);
		
	}

	@Override
	public ChargingStation createStation(ChargingStation station) {
		// TODO Auto-generated method stub
		return stationRepository.save(station);
	}
	 

}
