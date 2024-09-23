package com.ticketbooking.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = false)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
     Long id;

    @ManyToOne
    @JoinColumn(name = "conversation_id", referencedColumnName = "id")
     Conversation conversation;

    @Column(name = "sender")
     String sender; // "customer" hoặc "staff"

    @Column(name = "content")
    String content;

    @Column(name = "timestamp")
    LocalDateTime timestamp;
}
