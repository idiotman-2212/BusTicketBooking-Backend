package com.ticketbooking.repo;

import com.ticketbooking.model.Booking;
import com.ticketbooking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepo extends JpaRepository<Booking, Long> {

    @Query("""
            select b from Booking b
            where b.trip.id=:tripId 
            and b.paymentStatus <> 'CANCEL'
            """)
    List<Booking> getAllBookingFromTripAndDate(@Param("tripId") Long tripId);

    @Query(value = """
            select * from booking b where b.phone=:phone and b.username is null
            """, nativeQuery = true)
    List<Booking> findAllByPhone(@Param("phone") String phone);

    List<Booking> findAllByUser(User user);
}
