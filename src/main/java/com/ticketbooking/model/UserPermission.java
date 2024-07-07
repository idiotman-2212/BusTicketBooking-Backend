package com.ticketbooking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "UK_user_role_screen", columnNames = {"username", "role_id", "screenCode"})
})
@Builder
public class UserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String screenCode; // mã màn hình trên giao diện, tự quy ước sau

    @ManyToOne
    @JoinColumn(name = "username")
    @JsonIgnore
    private User user; // username

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
}
