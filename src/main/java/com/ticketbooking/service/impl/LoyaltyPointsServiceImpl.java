package com.ticketbooking.service.impl;

import com.ticketbooking.dto.LoyaltyTransactionDTO;
import com.ticketbooking.model.Booking;
import com.ticketbooking.model.LoyaltyTransaction;
import com.ticketbooking.model.User;
import com.ticketbooking.repo.BookingRepo;
import com.ticketbooking.repo.LoyaltyTransactionRepo;
import com.ticketbooking.repo.UserRepo;
import com.ticketbooking.service.LoyaltyPointsService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Transactional
@RequiredArgsConstructor
public class LoyaltyPointsServiceImpl implements LoyaltyPointsService {

    private static final BigDecimal POINTS_RATE = new BigDecimal("0.005");

    private final UserRepo userRepo;

    private final BookingRepo bookingRepo;

    private final LoyaltyTransactionRepo loyaltyTransactionRepo;

    @Override
    @Transactional
    public void earnPoints(Long bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

        User user = userRepo.findByUsername(booking.getUser().getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Tính toán số điểm dựa trên tổng thanh toán
        BigDecimal pointsEarned = booking.getTotalPayment().multiply(POINTS_RATE);

        // Đảm bảo pointsEarned không bị null hoặc lỗi
        if (pointsEarned == null || pointsEarned.compareTo(BigDecimal.ZERO) < 0) {
            pointsEarned = BigDecimal.ZERO;
        }

        // Gán pointsEarned cho đối tượng booking
        //booking.setPointsEarned(pointsEarned);
        bookingRepo.save(booking);  // Lưu lại đối tượng Booking sau khi gán điểm thưởng

        // Tạo giao dịch Loyalty
        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setUser(user);
        transaction.setBooking(booking);
        transaction.setAmount(pointsEarned);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setTransactionType(LoyaltyTransaction.TransactionType.EARN);
        loyaltyTransactionRepo.save(transaction);

        // Cộng điểm cho người dùng
        userRepo.addLoyaltyPoints(user.getUsername(), pointsEarned);
    }

    @Override
    @Transactional
    public void usePoints(Long bookingId, BigDecimal pointsToUse) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

        User user = userRepo.findByUsername(booking.getUser().getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (user.getLoyaltyPoints().compareTo(pointsToUse) < 0) {
            throw new IllegalArgumentException("Not enough loyalty points");
        }

        //booking.setPointsUsed(pointsToUse);
        booking.setTotalPayment(booking.getTotalPayment().subtract(pointsToUse));
        bookingRepo.save(booking);

        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setUser(user);
        transaction.setBooking(booking);
        transaction.setAmount(pointsToUse.negate());
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setTransactionType(LoyaltyTransaction.TransactionType.USE);
        loyaltyTransactionRepo.save(transaction);

        userRepo.deductLoyaltyPoints(user.getUsername(), pointsToUse);
    }

   @Override
    public BigDecimal getLoyaltyPoints(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        System.out.println("Username " + user.getUsername());
        return user.getLoyaltyPoints();
    }

    @Override
    public List<LoyaltyTransactionDTO> getLoyaltyTransactions(String username) {
        List<LoyaltyTransaction> transactions = loyaltyTransactionRepo.findByUserUsernameOrderById(username);

        return transactions.stream()
                .map(transaction -> {
                    LoyaltyTransactionDTO dto = new LoyaltyTransactionDTO();
                    dto.setId(transaction.getId());
                    dto.setBookingId(transaction.getBooking().getId());
                    dto.setCustFirstName(transaction.getBooking().getCustFirstName());
                    dto.setCustLastName(transaction.getBooking().getCustLastName());
                    dto.setAmount(transaction.getAmount());
                    dto.setTransactionDate(transaction.getTransactionDate());
                    dto.setTransactionType(transaction.getTransactionType().name()); // Chuyển đổi enum thành String
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
