package com.ticketbooking.BusTicketBooking.repository;

import com.ticketbooking.BusTicketBooking.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverRepo extends JpaRepository<Driver, Long> {
}