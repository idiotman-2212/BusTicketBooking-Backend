package com.ticketbooking.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = false)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    User customer;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    User staff;

    @OneToMany
    List<Message> messsages;
}
