package com.ticketbooking.BusTicketBooking.repository;

import com.ticketbooking.BusTicketBooking.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface DiscountRepo extends JpaRepository<Discount, Long> {

    Optional<Discount> findByCode(String code);

    @Query("""
                    select d from Discount d where d.endDateTime > current_timestamp 
            """)
    List<Discount> findAllAvailable();
}
