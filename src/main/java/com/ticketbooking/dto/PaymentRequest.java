package com.ticketbooking.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = false)
public class PaymentRequest {
     String orderInfo;
     long amount;
     String bankCode;
     String orderType;
     String locale;
}

