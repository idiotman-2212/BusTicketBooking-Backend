package com.ticketbooking.BusTicketBooking.exception;

import lombok.Data;

@Data
public class ResourceNotFoundException extends RuntimeException{
    private final String errorMessage;
}

