package com.ticketbooking.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ticketbooking.model.enumType.BookingType;
import com.ticketbooking.model.enumType.PaymentMethod;
import com.ticketbooking.model.enumType.PaymentStatus;
import com.ticketbooking.utils.AppConstants;
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
    @JoinColumn(name = "username")
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

    @Pattern(regexp = AppConstants.PHONE_REGEX_PATTERN, message = "Invalid phone")
    private String phone;

    @Pattern(regexp = AppConstants.EMAIL_REGEX_PATTERN, message = "Invalid email")
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
