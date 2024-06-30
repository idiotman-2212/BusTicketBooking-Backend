package com.ticketbooking.BusTicketBooking.exception;

import lombok.Data;

@Data
public class InvalidInputException extends RuntimeException {
    private final String errorMessage;
}
