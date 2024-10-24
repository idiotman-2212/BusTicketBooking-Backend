package com.ticketbooking.controller;

import com.ticketbooking.model.Location;
import com.ticketbooking.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    // Lấy danh sách tất cả các địa điểm
    @GetMapping
    public ResponseEntity<List<Location>> getAllLocations() {
        List<Location> locations = locationService.findAll();
        return new ResponseEntity<>(locations, HttpStatus.OK);
    }

    // Lấy danh sách các địa điểm theo id tỉnh thành
    @GetMapping("/province/{provinceId}")
    public ResponseEntity<List<Location>> getLocationsByProvince(@PathVariable Long provinceId) {
        List<Location> locations = locationService.findByProvinceId(provinceId);
        return new ResponseEntity<>(locations, HttpStatus.OK);
    }

    // Lấy thông tin cụ thể của một địa điểm theo id
    @GetMapping("/{id}")
    public ResponseEntity<Location> getLocationById(@PathVariable Long id) {
        Location location = locationService.findById(id);
        return new ResponseEntity<>(location, HttpStatus.OK);
    }

    // Thêm một địa điểm mới
    @PostMapping
    public ResponseEntity<Location> createLocation(@RequestBody Location location) {
        Location newLocation = locationService.saveLocation(location);
        return new ResponseEntity<>(newLocation, HttpStatus.CREATED);
    }

    // Cập nhật một địa điểm
    @PutMapping("/{id}")
    public ResponseEntity<Location> updateLocation(
            @PathVariable Long id,
            @RequestBody Location updatedLocation) {
        Location location = locationService.updateLocation(id, updatedLocation);
        return new ResponseEntity<>(location, HttpStatus.OK);
    }

    // Xóa một địa điểm theo id
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        locationService.deleteLocation(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
