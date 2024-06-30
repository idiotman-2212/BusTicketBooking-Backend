package com.ticketbooking.BusTicketBooking.payload.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class ReportResponse {

    private Map<String, ? extends Object> reportData;
}

