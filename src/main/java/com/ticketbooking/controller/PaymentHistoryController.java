package com.ticketbooking.controller;

import com.ticketbooking.model.PaymentHistory;
import com.ticketbooking.service.PaymentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/paymentHistories")
public class PaymentHistoryController {

    private final PaymentHistoryService paymentHistoryService;

    @GetMapping("/all/{id}")
    public List<PaymentHistory>  getHistoriesPaymentOfBooking(@PathVariable Long id) {
        return paymentHistoryService.findHistoriesByBookingId(id);
    }
}
