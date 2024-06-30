package com.ticketbooking.BusTicketBooking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ticketbooking.BusTicketBooking.entity.enumType.RoleCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private RoleCode roleCode;
    private String description;
    @OneToMany(mappedBy = "role")
    @JsonIgnore
    private List<UserPermission> permissions;
}
