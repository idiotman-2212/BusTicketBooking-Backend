package com.ticketbooking.service;

import com.ticketbooking.dto.BookingRequest;
import com.ticketbooking.dto.PageResponse;
import com.ticketbooking.model.Booking;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingService {

    List<Booking> findAllByPhone(String phone);

    List<Booking> findAllByUsername(String username);

    Booking findById(Long id);

    List<Booking> findAll();

    PageResponse<Booking> findAll(Integer page, Integer limit);

    List<Booking> save(BookingRequest bookingRequest);

    Booking update(Booking booking);

    String delete(Long id);

    List<Booking> getAllBookingFromTripAndDate(Long tripId);
}
