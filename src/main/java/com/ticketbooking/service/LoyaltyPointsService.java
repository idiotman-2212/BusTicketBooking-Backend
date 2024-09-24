package com.ticketbooking.service;

import com.ticketbooking.model.LoyaltyTransaction;

import java.math.BigDecimal;
import java.util.List;

public interface LoyaltyPointsService {
    List<LoyaltyTransaction> getLoyaltyTransactions(String username);
    BigDecimal getLoyaltyPoints(String username);
    void usePoints(Long bookingId, BigDecimal pointsToUse);
    void earnPoints(Long bookingId);
}
