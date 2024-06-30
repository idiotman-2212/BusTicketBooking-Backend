package com.ticketbooking.BusTicketBooking.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ticketbooking.BusTicketBooking.utils.AppConstants;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String firstName;
    private String lastName;

    @Column(unique = true)
    @Email(regexp = AppConstants.EMAIL_REGEX_PATTERN, message = "Invalid Email")
    private String email;

    @Column(unique = true)
    @Email(regexp = AppConstants.PHONE_REGEX_PATTERN, message = "Invalid Phone")
    private String phone;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    private Boolean gender;

    private String address;

    @Column(unique = true)
    private String licenseNumber;

    private Boolean quit;

    @OneToMany(mappedBy = "driver")
    @JsonIgnore
    private List<Trip> trips;

    public String getFullName() {
        return this.getFirstName().concat(" ").concat(this.getLastName());
    }
}
