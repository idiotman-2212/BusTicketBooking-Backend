package com.ticketbooking.BusTicketBooking.service;

import com.ticketbooking.BusTicketBooking.entity.Role;

import java.util.List;

public interface RoleService {
    Role findById(Long id);

    List<Role> findAll();

    Role update(Role role);
}
