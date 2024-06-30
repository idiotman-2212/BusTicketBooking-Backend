package com.ticketbooking.BusTicketBooking.controller;

import com.ticketbooking.BusTicketBooking.entity.Province;
import com.ticketbooking.BusTicketBooking.service.ProvinceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/provinces")
public class ProvinceController {

    private final ProvinceService provinceService;

    @GetMapping("/all")
    public List<Province> getAllProvinces() {
        return provinceService.findAll();
    }

}
