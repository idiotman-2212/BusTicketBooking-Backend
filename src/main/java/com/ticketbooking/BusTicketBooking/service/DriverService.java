package com.ticketbooking.BusTicketBooking.service;

import com.ticketbooking.BusTicketBooking.entity.Driver;
import com.ticketbooking.BusTicketBooking.payload.response.PageResponse;

import java.util.List;

public interface DriverService {
    Driver findById(Long id);

    List<Driver> findAll();

    PageResponse<Driver> findAll(Integer page, Integer limit);

    Driver save(Driver driver);

    Driver update(Driver driver);

    String delete(Long id);

    Boolean checkDuplicateDriverInfo(String mode, Object driverId, String field, String value);
}
