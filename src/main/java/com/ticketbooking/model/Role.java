package com.ticketbooking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ticketbooking.model.enumType.RoleCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private RoleCode roleCode;

    private String roleName;

    private String description;

    @OneToMany(mappedBy = "role")
    @JsonIgnore
    private List<UserPermission> permissions;

}
