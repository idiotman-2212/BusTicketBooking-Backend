package com.ticketbooking.BusTicketBooking.service;

import com.ticketbooking.BusTicketBooking.entity.Booking;
import com.ticketbooking.BusTicketBooking.payload.request.BookingRequest;
import com.ticketbooking.BusTicketBooking.payload.response.PageResponse;

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
