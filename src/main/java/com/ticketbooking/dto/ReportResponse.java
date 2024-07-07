package com.ticketbooking.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class ReportResponse {

    private Map<String, ? extends Object> reportData;
}
