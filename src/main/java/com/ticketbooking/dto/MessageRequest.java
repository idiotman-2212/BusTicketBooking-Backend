package com.ticketbooking.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = false)
public class MessageRequest {
     String sender;
     String content;
}
