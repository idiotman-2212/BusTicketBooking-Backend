package com.ticketbooking.BusTicketBooking.service;

import com.ticketbooking.BusTicketBooking.entity.Trip;
import com.ticketbooking.BusTicketBooking.payload.response.PageResponse;

import java.util.List;

public interface TripService {
    Trip findById(Long id);
    List<Trip> findAll();

    PageResponse<Trip> findAll(Integer page, Integer limit);

    Trip save(Trip trip);

    Trip update(Trip trip);

    String delete(Long id);

    List<Trip> findAllBySourceAndDest(Long sourceId, Long destId, String chosenFromDate, String chosenToDate);
}
