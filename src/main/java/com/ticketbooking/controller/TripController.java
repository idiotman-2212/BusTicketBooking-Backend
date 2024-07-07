package com.ticketbooking.controller;

import com.ticketbooking.dto.PageResponse;
import com.ticketbooking.model.Trip;
import com.ticketbooking.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/trips")
public class TripController {

    private final TripService tripService;

    @GetMapping("/all")
    public List<Trip> getAllTrips() {
        return tripService.findAll();
    }

    @GetMapping("/paging")
    public PageResponse<Trip> getPageOfTrips(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "5") Integer limit) {
        return tripService.findAll(page, limit);
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<Trip> getTrip(@PathVariable Long tripId) {
        return ResponseEntity
                .status(200)
                .body(tripService.findById(tripId));
    }

    @GetMapping("/{sourceId}/{destId}/{chosenFromDate}/{chosenToDate}")
    public List<Trip> findAllTripsBySourceAndDest(@PathVariable Long sourceId,
                                                  @PathVariable Long destId,
                                                  @PathVariable String chosenFromDate,
                                                  @PathVariable String chosenToDate) {
        return tripService.findAllBySourceAndDest(sourceId, destId, chosenFromDate, chosenToDate);
    }

    @PostMapping
    public ResponseEntity<Trip> createTrip(@RequestBody Trip trip) {
        return ResponseEntity
                .status(201)
                .body(tripService.save(trip));
    }

    @PutMapping
    public ResponseEntity<Trip> updateTrip(@RequestBody Trip trip) {
        return ResponseEntity
                .status(200)
                .body(tripService.update(trip));
    }

    @DeleteMapping("/{tripId}")
    public ResponseEntity<?> deleteTrip(@PathVariable Long tripId) {
        return ResponseEntity
                .status(200)
                .body(tripService.delete(tripId));
    }

}
