package com.ticketbooking.BusTicketBooking.repository;

import com.ticketbooking.BusTicketBooking.entity.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface PaymentHistoryRepo extends JpaRepository<PaymentHistory, Long> {

    List<PaymentHistory> findAllByBookingId(Long id);
}

