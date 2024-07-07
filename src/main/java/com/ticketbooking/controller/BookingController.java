package com.ticketbooking.controller;

import com.ticketbooking.dto.BookingRequest;
import com.ticketbooking.dto.PageResponse;
import com.ticketbooking.model.Booking;
import com.ticketbooking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/all")
    public List<Booking> getAllBookings() {
        return bookingService.findAll();
    }

    @GetMapping("/all/{phone}")
    public List<Booking> getAllBookingsByPhone(@PathVariable String phone) {
        return bookingService.findAllByPhone(phone);
    }

    @GetMapping("/all/user/{username}")
    public List<Booking> getAllBookingsByUsername(@PathVariable String username) {
        return bookingService.findAllByUsername(username);
    }

    @GetMapping("/paging")
    public PageResponse<Booking> getPageOfBookings(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "5") Integer limit) {
        return bookingService.findAll(page, limit);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Booking> getBooking(@PathVariable Long bookingId) {
        return ResponseEntity
                .status(200)
                .body(bookingService.findById(bookingId));
    }

    @GetMapping("/emptySeats")
    public List<Booking> getAllBookingFromTripAndDate(@RequestParam Long tripId) {
        return bookingService.getAllBookingFromTripAndDate(tripId);
    }

    @PostMapping()
    public ResponseEntity<List<Booking>> createBookings(@RequestBody BookingRequest bookingRequest) {
        return ResponseEntity
                .status(201)
                .body(bookingService.save(bookingRequest));
    }

    @PutMapping
    public ResponseEntity<Booking> updateTrip(@RequestBody Booking booking) {
        return ResponseEntity
                .status(200)
                .body(bookingService.update(booking));
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<?> deleteBooking(@PathVariable Long bookingId) {
        return ResponseEntity
                .status(200)
                .body(bookingService.delete(bookingId));
    }
}
