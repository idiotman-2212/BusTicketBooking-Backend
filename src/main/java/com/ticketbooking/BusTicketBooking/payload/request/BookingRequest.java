package com.ticketbooking.BusTicketBooking.payload.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ticketbooking.BusTicketBooking.entity.Trip;
import com.ticketbooking.BusTicketBooking.entity.User;
import com.ticketbooking.BusTicketBooking.entity.enumType.BookingType;
import com.ticketbooking.BusTicketBooking.entity.enumType.PaymentMethod;
import com.ticketbooking.BusTicketBooking.entity.enumType.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {
    private int id;
    private User user;
    private Trip trip;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime bookingDateTime;

    private String[] seatNumber;
    private BookingType bookingType;
    private String pickUpAddress;

    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private BigDecimal totalPayment;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentDateTime;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
}
