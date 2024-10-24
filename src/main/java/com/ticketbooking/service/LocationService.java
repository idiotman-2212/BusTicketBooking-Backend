package com.ticketbooking.service;

import com.ticketbooking.model.Location;

import java.util.List;

public interface LocationService {
    Location findById(Long id);

    List<Location> findAll();

    List<Location> findByProvinceId(Long provinceId);

    Location saveLocation(Location location);

    Location updateLocation(Long id, Location updatedLocation);

    void deleteLocation(Long id);
}
