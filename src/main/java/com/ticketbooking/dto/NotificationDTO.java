package com.ticketbooking.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = false)
@Builder
public class NotificationDTO {
     Long id;
     String title;
     String message;
     LocalDateTime createdAt;
     boolean isRead;

     // Thông tin từ User
      String username;
      String email;
      String firstName;
      String lastName;

     // Thông tin từ Trip
      Long tripId;
      String source;
      String destination;
      LocalDateTime departureTime;
      BigDecimal price;
}
