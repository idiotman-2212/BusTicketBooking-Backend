package com.ticketbooking.BusTicketBooking.repository;

import com.ticketbooking.BusTicketBooking.entity.Booking;
import com.ticketbooking.BusTicketBooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
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
