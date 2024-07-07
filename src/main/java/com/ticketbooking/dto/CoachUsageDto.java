package com.ticketbooking.dto;

import com.ticketbooking.model.enumType.CoachType;
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
