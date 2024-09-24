package com.ticketbooking.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.ticketbooking.utils.AppConstants;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = false)
public class User implements UserDetails {
    @Id
     String username;

    @NotEmpty(message = "Password should not be empty")
     String password;

    @NotEmpty(message = "First name should not be empty")
     String firstName;

    @NotEmpty(message = "Last name should not be empty")
     String lastName;

    @Column(unique = true)
    @Email(regexp = AppConstants.EMAIL_REGEX_PATTERN, message = "Invalid email")
     String email;

    @Column(unique = true)
    @Pattern(regexp = AppConstants.PHONE_REGEX_PATTERN, message = "Invalid phone")
     String phone;

    @JsonFormat(pattern = "yyyy-MM-dd")
     LocalDate dob;

     Boolean gender;

     String address;

     Boolean active;

     //Tích xu
    @Column(name = "loyalty_points", nullable = false, columnDefinition = "decimal(38,2) default 0")
     BigDecimal loyaltyPoints;

    @JsonManagedReference
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
    @JsonIgnore
    List<LoyaltyTransaction> loyaltyTransactions;

    public void addLoyaltyPoints(BigDecimal points) {
        this.loyaltyPoints = this.loyaltyPoints.add(points);
    }

    public void deductLoyaltyPoints(BigDecimal points) {
        if (this.loyaltyPoints.compareTo(points) < 0) {
            throw new IllegalArgumentException("Not enough loyalty points");
        }
        this.loyaltyPoints = this.loyaltyPoints.subtract(points);
    }


    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
     List<UserPermission> permissions;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
     List<Booking> bookings;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
    @JsonIgnore
     List<Token> tokens;

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this
                .getPermissions()
                .stream()
                .map(role -> new SimpleGrantedAuthority(role.getRole().getRoleCode().name()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
