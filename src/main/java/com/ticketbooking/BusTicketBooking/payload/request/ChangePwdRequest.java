package com.ticketbooking.BusTicketBooking.payload.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePwdRequest {
    private String username;
    private String newPassword;
}
