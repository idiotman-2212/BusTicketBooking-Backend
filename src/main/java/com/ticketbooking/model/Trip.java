package com.ticketbooking.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "UK_trip_fields", columnNames = {
                "driver_id", "coach_id", "source_id", "dest_id", "departureDateTime"
        })
})
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne
    @JoinColumn(name = "coach_id")
    private Coach coach;

    @ManyToOne
    @JoinColumn(name = "source_id")
    private Province source;

    @ManyToOne
    @JoinColumn(name = "dest_id")
    private Province destination;

    @ManyToOne
    @JoinColumn(name = "discount_id")
    private Discount discount;

    @Min(value = 0, message = "Price should be positive")
    private BigDecimal price;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime departureDateTime;

    private Double duration;

    @OneToMany(mappedBy = "trip")
    @JsonIgnore
    private List<Booking> bookings;
}

