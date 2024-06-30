package com.ticketbooking.BusTicketBooking.service;

import com.ticketbooking.BusTicketBooking.entity.Booking;
import com.ticketbooking.BusTicketBooking.entity.User;
import com.ticketbooking.BusTicketBooking.payload.dto.PermissionDto;
import com.ticketbooking.BusTicketBooking.payload.dto.ScreenPermissionDto;
import com.ticketbooking.BusTicketBooking.payload.response.PageResponse;

import java.security.Permission;
import java.util.List;

public interface UserService {
    User findByUsername(String username);
    List<User> findAll();
    PageResponse<User> findAll(Integer page, Integer limit);
    User save(User user);
    User update(User user);
    String delete(String username);
    Boolean checkDuplicateUserInfo(String mode, String username, String field, String value);
    PermissionDto getUserPermission(String username);
    PermissionDto updateUserScreenPermission(ScreenPermissionDto screenPermissionDto);
}
