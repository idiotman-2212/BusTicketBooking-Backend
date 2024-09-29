package com.ticketbooking.service.impl;

import com.ticketbooking.dto.PageResponse;
import com.ticketbooking.exception.ExistingResourceException;
import com.ticketbooking.exception.InvalidInputException;
import com.ticketbooking.exception.ResourceNotFoundException;
import com.ticketbooking.model.Booking;
import com.ticketbooking.model.LoyaltyTransaction;
import com.ticketbooking.model.Trip;
import com.ticketbooking.model.User;
import com.ticketbooking.model.enumType.PaymentStatus;
import com.ticketbooking.repo.BookingRepo;
import com.ticketbooking.repo.LoyaltyTransactionRepo;
import com.ticketbooking.repo.TripRepo;
import com.ticketbooking.repo.UserRepo;
import com.ticketbooking.service.TripService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TripRepo tripRepo;
    private final BookingRepo bookingRepo;
    private final UserRepo userRepo;
    private final LoyaltyTransactionRepo loyaltyTransactionRepo;

    @Override
    public Trip findById(Long id) {
        return tripRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found Trip<%d>".formatted(id)));
    }

    @Override
    @Cacheable(cacheNames = {"trips"})
    public List<Trip> findAll() {
        return tripRepo.findAll();
    }

    @Override
    @Cacheable(cacheNames = {"trips_paging"}, key = "{#page, #limit}")
    public PageResponse<Trip> findAll(Integer page, Integer limit) {
        Page<Trip> pageSlice = tripRepo.findAll(PageRequest.of(page, limit));
        PageResponse<Trip> pageResponse = new PageResponse<>();
        pageResponse.setDataList(pageSlice.getContent());
        pageResponse.setPageCount(pageSlice.getTotalPages());
        pageResponse.setTotalElements(pageSlice.getTotalElements());
        return pageResponse;
    }

    @Override
    public List<Trip> getIncompleteTrips() {
        return tripRepo.findByCompletedFalse();
    }

    @Override
    @Transactional
    public void completeTrip(Long tripId) {
        // Tìm chuyến đi theo ID
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with ID: " + tripId));

        // Tính toán thời gian hoàn thành dự kiến của chuyến đi
        double duration = trip.getDuration();
        long hours = (long) duration;
        long minutes = (long) ((duration - hours) * 60);

        LocalDateTime estimatedCompletionTime = trip.getDepartureDateTime()
                .plusHours(hours)
                .plusMinutes(minutes);

        // Kiểm tra xem chuyến đi đã hoàn thành hay chưa (dựa vào thời gian hiện tại)
        if (LocalDateTime.now().isBefore(estimatedCompletionTime)) {
            throw new InvalidInputException("Trip has not been completed yet.");
        }

        // Đánh dấu chuyến đi đã hoàn thành
        trip.setCompleted(true);
        tripRepo.save(trip);

        System.out.println("Trip with ID " + tripId + " has been marked as completed.");

        // Lấy danh sách các booking liên quan đến chuyến đi này
        List<Booking> bookings = bookingRepo.getAllBookingFromTripAndDate(tripId);

        for (Booking booking : bookings) {
            User user = booking.getUser();

            // Kiểm tra nếu booking không có người dùng liên quan
            if (user == null) {
                System.out.println("Booking " + booking.getId() + " does not have a user associated with it. No points will be earned.");
                continue;
            }

            // Kiểm tra xem giao dịch đã được tạo cho booking này chưa
            boolean transactionExists = loyaltyTransactionRepo.existsByBookingId(booking.getId());

            // Điều kiện: Trạng thái thanh toán là PAID và chưa tồn tại giao dịch trước đó
            if (booking.getPaymentStatus() == PaymentStatus.PAID && !transactionExists) {
                // Tính điểm thưởng dựa trên 1% tổng giá trị thanh toán
                BigDecimal pointsEarned = booking.getTotalPayment().multiply(new BigDecimal("0.01"));
                booking.setPointsEarned(pointsEarned);

                System.out.println("Booking " + booking.getId() + " is PAID and no previous transaction exists.");
                System.out.println("Points earned for booking " + booking.getId() + ": " + pointsEarned);

                // Cộng điểm xu vào tài khoản của người dùng
                user.addLoyaltyPoints(pointsEarned);

                // Tạo giao dịch tích điểm vào bảng LoyaltyTransaction
                LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                        .user(user)
                        .booking(booking)
                        .amount(pointsEarned)
                        .transactionDate(LocalDateTime.now())
                        .transactionType(LoyaltyTransaction.TransactionType.EARN)
                        .build();

                // Lưu các thay đổi vào các repo
                userRepo.save(user);
                loyaltyTransactionRepo.save(transaction);
                bookingRepo.save(booking);

                System.out.println("Loyalty points and transaction saved for booking " + booking.getId());
            } else {
                System.out.println("Booking " + booking.getId() + " is not eligible for points.");
            }
        }
    }



    @Override
    @Transactional
    @CacheEvict(cacheNames = {"trips", "trips_paging"}, allEntries = true)
    public Trip save(Trip trip) {

        // check if source == destination
        if (trip.getSource().getId() == trip.getDestination().getId()) {
            throw new InvalidInputException("Start location <%s> and End location <%s> cannot be the same"
                    .formatted(
                            trip.getSource().getName(),
                            trip.getDestination().getName()
                    )
            );
        }

        // Check if the driver has any trip within the last 2 days
        LocalDateTime twoDaysAgo = trip.getDepartureDateTime().minusDays(2);
        System.out.println("Two days ago: " + twoDaysAgo);

        List<Trip> recentTrips = tripRepo.findRecentTripsByDriverId(
                trip.getDriver().getId(),
                twoDaysAgo,
                trip.getDepartureDateTime()
        );

        if (!recentTrips.isEmpty()) {
            System.out.println("Found recent trips: " + recentTrips.size());
            throw new InvalidInputException("Driver <%s> has another trip within 2 days of the new trip."
                    .formatted(trip.getDriver().getFullName()));
        }

        // check duplicate Trip
        List<Trip> duplicateTrips = tripRepo.findDuplicateDepartureTimeTrip(
                trip.getDriver().getId(),
                trip.getCoach().getId(),
                trip.getSource().getId(),
                trip.getDestination().getId(),
                trip.getDepartureDateTime());

        if (!duplicateTrips.isEmpty()) {
            Trip duplicateTrip = duplicateTrips.get(0);
            String duplicateMsg = "Trip Driver<%s>, Coach<%s>, From<%s> To<%s>, At<%s> is existed!"
                    .formatted(
                            duplicateTrip.getDriver().getFullName(),
                            duplicateTrip.getCoach().getName(),
                            duplicateTrip.getSource().getName(),
                            duplicateTrip.getDestination().getName(),
                            duplicateTrip.getDepartureDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    );
            throw new ExistingResourceException(duplicateMsg);
        }

        return tripRepo.save(trip);
    }


    @Override
    @Transactional
    @CacheEvict(cacheNames = {"trips", "trips_paging"}, allEntries = true)
    public Trip update(Trip trip) {
        Trip existingTrip = findById(trip.getId());

        // Nếu trạng thái completed chuyển từ false sang true, gọi phương thức completeTrip
        if (!existingTrip.getCompleted() && trip.getCompleted()) {
            completeTrip(trip.getId());
        }

        // Cập nhật các thông tin khác của chuyến đi
        existingTrip.setPrice(trip.getPrice());
        existingTrip.setDuration(trip.getDuration());
        existingTrip.setSource(trip.getSource());
        existingTrip.setDestination(trip.getDestination());
        existingTrip.setDriver(trip.getDriver());
        existingTrip.setCoach(trip.getCoach());
        existingTrip.setDiscount(trip.getDiscount());
        existingTrip.setDepartureDateTime(trip.getDepartureDateTime());
        existingTrip.setCompleted(trip.getCompleted());

        return tripRepo.save(existingTrip);
    }


    private boolean businessConditionsFailed(Trip trip, Trip existingTrip) {
        // Kiểm tra xem tài xế có chuyến đi trong 2 ngày tới không
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);
        List<Trip> recentTrips = tripRepo.findRecentTripsByDriverId(
                trip.getDriver().getId(), twoDaysAgo, LocalDateTime.now());
        return !recentTrips.isEmpty();
    }


    @Override
    @CacheEvict(cacheNames = {"trips", "trips_paging"}, allEntries = true)
    public String delete(Long id) {

        Trip foundTrip = findById(id);

        if (!foundTrip.getBookings().isEmpty()) {
            throw new ExistingResourceException("Trip<%d> is in used, can't be deleted".formatted(id));
        }

        tripRepo.deleteById(id);

        return "Delete Trip<%d> successfully".formatted(id);
    }

    @Override
    public List<Trip> findAllBySourceAndDest(Long sourceId, Long destId, String chosenFromDate, String chosenToDate) {
        LocalDate fromDate = LocalDate.parse(chosenFromDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate toDate = LocalDate.parse(chosenToDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return tripRepo.findAllBySourceIdAndDestinationId(sourceId, destId, fromDate, toDate);
    }

    @Override
    public List<Trip> findRecentTripsByDriverId(Long driverId, String fromDateTime, String toDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime fromDateTimeParsed = LocalDateTime.parse(fromDateTime, formatter);
        LocalDateTime toDateTimeParsed = LocalDateTime.parse(toDateTime, formatter);
        return tripRepo.findRecentTripsByDriverId(driverId, fromDateTimeParsed, toDateTimeParsed);
    }

    // Phương thức tự động cập nhật trạng thái completed cho các chuyến đi chưa hoàn thành
    @Scheduled(fixedRate = 3600000) // Chạy mỗi giờ
    public void updateCompletedTrips() {
        List<Trip> incompleteTrips = tripRepo.findByCompletedFalse(); // Lấy các chuyến đi chưa hoàn thành

        for (Trip trip : incompleteTrips) {
            double duration = trip.getDuration();
            long hours = (long) duration;
            long minutes = (long) ((duration - hours) * 60);

            LocalDateTime estimatedCompletionTime = trip.getDepartureDateTime()
                    .plusHours(hours)
                    .plusMinutes(minutes);

            // Nếu thời gian hiện tại lớn hơn thời gian hoàn thành dự kiến
            if (LocalDateTime.now().isAfter(estimatedCompletionTime)) {
                trip.setCompleted(true);
                completeTrip(trip.getId()); // Gọi phương thức hoàn thành chuyến đi và tích điểm
                tripRepo.save(trip);
            }
        }
    }

}