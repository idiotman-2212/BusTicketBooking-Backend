package com.ticketbooking.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ticketbooking.utils.AppConstants;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
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
    @Email(regexp = AppConstants.EMAIL_REGEX_PATTERN, message = "Invalid email")
    private String email;

    @Column(unique = true)
    @Pattern(regexp = AppConstants.PHONE_REGEX_PATTERN, message = "Invalid phone")
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
