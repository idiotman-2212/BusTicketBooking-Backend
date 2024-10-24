package com.ticketbooking.service.impl;

import com.ticketbooking.exception.ResourceNotFoundException;
import com.ticketbooking.model.Location;
import com.ticketbooking.repo.LocationRepo;
import com.ticketbooking.service.LocationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepo locationRepo;

    @Override
    @Transactional
    public Location findById(Long id) {
        return locationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with ID: " + id));
    }

    @Override
    @Transactional
    public List<Location> findAll() {
        return locationRepo.findAll();
    }

    @Override
    @Transactional
    public List<Location> findByProvinceId(Long provinceId) {
        return locationRepo.findByProvinceId(provinceId);
    }

    @Override
    @Transactional
    public Location saveLocation(Location location) {
        return locationRepo.save(location);
    }

    @Override
    @Transactional
    public Location updateLocation(Long id, Location updatedLocation) {
        Location existingLocation = findById(id);

        existingLocation.setAddress(updatedLocation.getAddress());
        existingLocation.setWard(updatedLocation.getWard());
        existingLocation.setDistrict(updatedLocation.getDistrict());
        existingLocation.setProvince(updatedLocation.getProvince());

        return locationRepo.save(existingLocation);
    }

    @Override
    @Transactional
    public void deleteLocation(Long id) {
        Location existingLocation = findById(id);
        locationRepo.delete(existingLocation);
    }
}
