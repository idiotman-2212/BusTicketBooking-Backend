package com.ticketbooking.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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


    @Column(name = "points_earned", nullable = false, columnDefinition = "decimal(38,2) default 0")
    private BigDecimal pointsEarned;

    @Column(name = "points_used", nullable = false, columnDefinition = "decimal(38,2) default 0")
    private BigDecimal pointsUsed;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<LoyaltyTransaction> loyaltyTransactions;

    // Phương thức áp dụng xu giảm giá
    public void applyLoyaltyPoints(BigDecimal points) {
        if (points.compareTo(BigDecimal.ZERO) > 0) {
            this.pointsUsed = points;
            this.totalPayment = this.totalPayment.subtract(points);
        }
    }
    // Phương thức tính toán số xu tích lũy
     public void calculateEarnedPoints(BigDecimal rate) {
          this.pointsEarned = (this.totalPayment != null && rate != null)
            ? this.totalPayment.multiply(rate)
            : BigDecimal.ZERO;
      }

}
