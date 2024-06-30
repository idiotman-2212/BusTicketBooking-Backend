package com.ticketbooking.BusTicketBooking.repository;

import com.ticketbooking.BusTicketBooking.entity.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProvinceRepo extends JpaRepository<Province, Long> {

}