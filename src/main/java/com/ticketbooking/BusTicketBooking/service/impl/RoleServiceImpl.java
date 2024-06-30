package com.ticketbooking.BusTicketBooking.service.impl;


import com.ticketbooking.BusTicketBooking.entity.Driver;
import com.ticketbooking.BusTicketBooking.entity.Role;
import com.ticketbooking.BusTicketBooking.exception.ResourceNotFoundException;
import com.ticketbooking.BusTicketBooking.repository.RoleRepo;
import com.ticketbooking.BusTicketBooking.repository.UtilRepo;
import com.ticketbooking.BusTicketBooking.service.RoleService;
import com.ticketbooking.BusTicketBooking.validator.ObjectValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepo roleRepo;
    private final ObjectValidator<Role> objectValidator;

    private final UtilRepo utilRepo;

    @Override
    public Role findById(Long id) {
        return roleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found Role<%d>".formatted(id)));
    }

    @Override
    public List<Role> findAll() {
        return roleRepo.findAll();
    }

    @Override
    public Role update(Role role) {
        return null;
    }
}
