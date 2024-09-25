package com.ticketbooking.controller;

import com.ticketbooking.dto.LoyaltyTransactionDTO;
import com.ticketbooking.model.LoyaltyTransaction;
import com.ticketbooking.service.LoyaltyPointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
public class LoyaltyPointsController {

    private final LoyaltyPointsService loyaltyPointsService;

    @GetMapping("/points")
    public ResponseEntity<BigDecimal> getLoyaltyPoints(Authentication authentication) {
        String username = authentication.getName();
        BigDecimal points = loyaltyPointsService.getLoyaltyPoints(username);
        return ResponseEntity.ok(points);
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getLoyaltyTransactions(Authentication authentication) {
        String username = authentication.getName();
        List<LoyaltyTransactionDTO> transactions = loyaltyPointsService.getLoyaltyTransactions(username);
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/use")
    public ResponseEntity<Void> usePoints(@RequestParam Long bookingId, @RequestParam BigDecimal points) {
        loyaltyPointsService.usePoints(bookingId, points);
        return ResponseEntity.ok().build();
    }

    // This endpoint should be called after a booking is completed and the trip has been taken
    @PostMapping("/earn")
    public ResponseEntity<Void> earnPoints(@RequestParam Long bookingId) {
        loyaltyPointsService.earnPoints(bookingId);
        return ResponseEntity.ok().build();
    }
}