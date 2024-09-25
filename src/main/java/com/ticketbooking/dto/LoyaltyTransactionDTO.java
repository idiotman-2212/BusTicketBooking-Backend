package com.ticketbooking.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = false)
public class LoyaltyTransactionDTO {
     Long id;
     Long bookingId; // Thay vì toàn bộ đối tượng booking
     String custFirstName; // Tên khách hàng
     String custLastName; // Họ khách hàng
     BigDecimal amount; // Số tiền giao dịch
     LocalDateTime transactionDate; // Ngày giao dịch
     String transactionType; // Loại giao dịch (EARN, USE, EXPIRE)
}
