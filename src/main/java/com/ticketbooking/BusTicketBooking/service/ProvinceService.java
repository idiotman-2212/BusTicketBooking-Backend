package com.ticketbooking.BusTicketBooking.service;

import com.ticketbooking.BusTicketBooking.entity.Province;

import java.util.List;

public interface ProvinceService {
    Province findById(Long id);

    Province findByCodeName(String codeName);

    List<Province> findAll();
}
