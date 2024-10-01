package com.ticketbooking.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "Notification")
@Data

@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = false)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
     Long id;

    @ManyToOne
    @JoinColumn(name = "username", referencedColumnName = "username", nullable = false)
    @JsonIgnore
     User user;

    @ManyToOne
    @JoinColumn(name = "trip_id", referencedColumnName = "id")
    @JsonIgnore
     Trip trip;

    String title;
    String message;

    @Column(name = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
     LocalDateTime createdAt;

    @Column(name = "is_read", nullable = false)
     boolean isRead = false;
}