package com.ticketbooking.BusTicketBooking.payload.dto;

import com.ticketbooking.BusTicketBooking.entity.enumType.CoachType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoachUsageDto {

    private CoachType coachType;

    private Long usage;
}
