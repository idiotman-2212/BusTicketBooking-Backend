package com.ticketbooking.BusTicketBooking.service.impl;

import com.ticketbooking.BusTicketBooking.entity.Province;
import com.ticketbooking.BusTicketBooking.exception.ResourceNotFoundException;
import com.ticketbooking.BusTicketBooking.repository.ProvinceRepo;
import com.ticketbooking.BusTicketBooking.service.ProvinceService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProvinceServiceImpl implements ProvinceService {

    private final ProvinceRepo provinceRepo;

    @Override
    public Province findById(Long id) {
        return provinceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found Province<%d>".formatted(id)));
    }

    @Override
    public Province findByCodeName(String codeName) {
        return null;
    }

    @Override
    @Cacheable(cacheNames = {"provinces"})
    public List<Province> findAll() {
        return provinceRepo.findAll();
    }
}

