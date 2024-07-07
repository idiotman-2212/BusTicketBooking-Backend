package com.ticketbooking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Province {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // eg: 'Ninh Thuận'

    private String codeName; // eg: 'ninh_thuan'

    @OneToMany(mappedBy = "source")
    @JsonIgnore
    private List<Trip> sourceTrips;

    @OneToMany(mappedBy = "destination")
    @JsonIgnore
    private List<Trip> destTrips;
}
