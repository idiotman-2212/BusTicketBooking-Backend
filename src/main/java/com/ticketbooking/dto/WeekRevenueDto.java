package com.ticketbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeekRevenueDto {
    private Integer year;
    private Integer month;
    private Integer day;
    private BigDecimal totalRevenue;
}
