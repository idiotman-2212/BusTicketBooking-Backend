package com.ticketbooking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ticketbooking.model.Trip;
import com.ticketbooking.model.User;
import com.ticketbooking.model.enumType.BookingType;
import com.ticketbooking.model.enumType.PaymentMethod;
import com.ticketbooking.model.enumType.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookingRequest {

    private Long id;

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
