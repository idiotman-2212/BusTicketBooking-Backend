package com.ticketbooking.BusTicketBooking.repository;

import com.ticketbooking.BusTicketBooking.entity.Booking;
import com.ticketbooking.BusTicketBooking.payload.dto.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReportRepo extends JpaRepository<Booking, Long> {

    @Query(value = """
                select new com.ticketbooking.BusTicketBooking.payload.dto.YearRevenueDto(year(b.paymentDateTime), sum(b.totalPayment))
                from Booking b
                where year(b.paymentDateTime) between :startYear and :endYear and b.paymentStatus='PAID'
                group by year(b.paymentDateTime)
            """)
    List<YearRevenueDto> getYearTotalRevenue(@Param("startYear") Integer startYear, @Param("endYear") Integer endYear);

    @Query(value = """
            select new com.ticketbooking.BusTicketBooking.payload.dto.MonthRevenueDto(year(b.paymentDateTime), month(b.paymentDateTime), sum(b.totalPayment))
            from Booking b
            where date(b.paymentDateTime) between :startDate and :endDate and b.paymentStatus='PAID'
            group by year(b.paymentDateTime), month(b.paymentDateTime)
            """)
    List<MonthRevenueDto> getMonthTotalRevenue(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = """
            select new com.ticketbooking.BusTicketBooking.payload.dto.WeekRevenueDto(year(b.paymentDateTime), month(b.paymentDateTime),
             day(b.paymentDateTime), sum(b.totalPayment))
            from Booking b
            where date(b.paymentDateTime) between :startDate and :endDate and b.paymentStatus='PAID'
            group by year(b.paymentDateTime), month(b.paymentDateTime), day(b.paymentDateTime)
            """)
    List<WeekRevenueDto> getWeekTotalRevenue(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = """
            select new com.ticketbooking.BusTicketBooking.payload.dto.CoachUsageDto(b.trip.coach.coachType, count(b.trip.coach.coachType))
            from Booking b
            where date(b.bookingDateTime) between :startDate and :endDate and b.paymentStatus='PAID'
            group by b.trip.coach.coachType
            """)
    List<CoachUsageDto> getCoachUsage(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);


    @Query(value = """
            select new com.ticketbooking.BusTicketBooking.payload.dto.TopRouteDto(concat(b.trip.source.name, ' - ', b.trip.destination.name), count(b.trip.id))
            from Booking b
            where date(b.bookingDateTime) between :startDate and :endDate and b.paymentStatus='PAID'
            group by concat(b.trip.source.name, ' - ', b.trip.destination.name)
            order by count(b.trip.id) desc
            limit 5
            """)
    List<TopRouteDto> getMonthTopRoute(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
