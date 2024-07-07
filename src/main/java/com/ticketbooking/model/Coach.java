package com.ticketbooking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ticketbooking.model.enumType.CoachType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name; // Số hiệu xe, eg: 'Tài 1', 'Tài 2'

    private Integer capacity;

    @Column(unique = true)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    private CoachType coachType;

    @OneToMany(mappedBy = "coach")
    @JsonIgnore
    private List<Trip> trips;
}
