package com.ticketbooking.BusTicketBooking.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ticketbooking.BusTicketBooking.entity.enumType.BookingType;
import com.ticketbooking.BusTicketBooking.entity.enumType.PaymentMethod;
import com.ticketbooking.BusTicketBooking.entity.enumType.PaymentStatus;
import com.ticketbooking.BusTicketBooking.utils.AppConstants;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime bookingDateTime;

    private String seatNumber;

    @Enumerated(EnumType.STRING)
    private BookingType bookingType;

    private String pickUpAddress;
    private String custFirstName;
    private String custLastName;

    @Pattern(regexp = AppConstants.PHONE_REGEX_PATTERN, message = "Invalid Phone")
    private String phone;

    @Pattern(regexp = AppConstants.EMAIL_REGEX_PATTERN, message = "Invalid Email")
    private String email;

    private BigDecimal totalPayment;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentDateTime;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @OneToMany(mappedBy = "booking")
    private List<PaymentHistory> paymentHistories;
}
