package com.ticketbooking.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = false)
public class MessageResponse {
    String sender;
    String content;
    LocalDateTime timestamp;
}
