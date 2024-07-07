package com.ticketbooking.service.impl;

import com.ticketbooking.dto.BookingRequest;
import com.ticketbooking.dto.PageResponse;
import com.ticketbooking.exception.ResourceNotFoundException;
import com.ticketbooking.model.Booking;
import com.ticketbooking.model.PaymentHistory;
import com.ticketbooking.model.User;
import com.ticketbooking.model.enumType.PaymentStatus;
import com.ticketbooking.repo.BookingRepo;
import com.ticketbooking.repo.PaymentHistoryRepo;
import com.ticketbooking.repo.UserRepo;
import com.ticketbooking.service.BookingService;
import com.ticketbooking.service.PaymentHistoryService;
import com.ticketbooking.validator.ObjectValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepo bookingRepo;

    private final PaymentHistoryRepo paymentHistoryRepo;

    private final ObjectValidator<Booking> objectValidator;

    private final UserRepo userRepo;

    @Override
    @Cacheable(cacheNames = {"bookings"}, key = "#phone")
    public List<Booking> findAllByPhone(String phone) {
        return bookingRepo.findAllByPhone(phone);
    }

    @Override
    @Cacheable(cacheNames = {"bookings"}, key = "#username")
    public List<Booking> findAllByUsername(String username) {
        User foundUser = userRepo.findByUsername(username).get();
        return bookingRepo.findAllByUser(foundUser);
    }

    @Override
    public Booking findById(Long id) {
        return bookingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found Booking<%d>".formatted(id)));
    }

    @Override
    @Cacheable(cacheNames = {"bookings"})
    public List<Booking> findAll() {
        return bookingRepo.findAll();
    }

    @Override
    @Cacheable(cacheNames = {"bookings_paging"}, key = "{#page, #limit}")
    public PageResponse<Booking> findAll(Integer page, Integer limit) {
        Page<Booking> pageSlice = bookingRepo.findAll(PageRequest.of(page, limit));
        PageResponse<Booking> pageResponse = new PageResponse<>();
        pageResponse.setDataList(pageSlice.getContent());
        pageResponse.setPageCount(pageSlice.getTotalPages());
        pageResponse.setTotalElements(pageSlice.getTotalElements());
        return pageResponse;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"bookings", "bookings_paging"}, allEntries = true)
    public List<Booking> save(BookingRequest bookingRequest) {

        String[] selectSeats = bookingRequest.getSeatNumber();

        List<Booking> orderedBookings = new ArrayList<>();
        for (String seat : selectSeats) {

            orderedBookings.add(Booking
                    .builder()
                    .user(bookingRequest.getUser())
                    .trip(bookingRequest.getTrip())
                    .bookingDateTime(bookingRequest.getBookingDateTime())
                    .seatNumber(seat)
                    .bookingType(bookingRequest.getBookingType())
                    .pickUpAddress(bookingRequest.getPickUpAddress())
                    .custFirstName(bookingRequest.getFirstName())
                    .custLastName(bookingRequest.getLastName())
                    .phone(bookingRequest.getPhone())
                    .email(bookingRequest.getEmail())
                    .totalPayment(BigDecimal.valueOf(bookingRequest.getTotalPayment().longValue() / selectSeats.length))
                    .paymentDateTime(LocalDateTime.now())
                    .paymentMethod(bookingRequest.getPaymentMethod())
                    .paymentStatus(bookingRequest.getPaymentStatus())
                    .build());
        }

        var savedBookings = bookingRepo.saveAll(orderedBookings);

        List<PaymentHistory> paymentHistories = new ArrayList<>();

        for (Booking savedBooking : savedBookings) {
            paymentHistories.add(PaymentHistory
                    .builder()
                    .booking(savedBooking)
                    .oldStatus(null)
                    .newStatus(savedBooking.getPaymentStatus())
                    .statusChangeDateTime(savedBooking.getPaymentDateTime())
                    .build());
        }

        paymentHistoryRepo.saveAll(paymentHistories);

        return savedBookings;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"bookings", "bookings_paging"}, allEntries = true)
    public Booking update(Booking booking) {
        Booking foundBooking = findById(booking.getId());
        PaymentStatus oldPaymentStatus = foundBooking.getPaymentStatus();
        PaymentStatus newPaymentStatus = booking.getPaymentStatus();
        // unpaid -> unpaid: don't create payment history change
        if (oldPaymentStatus.equals(newPaymentStatus)) {
            return booking;
        }

        paymentHistoryRepo.save(PaymentHistory
                .builder()
                .oldStatus(oldPaymentStatus)
                .newStatus(newPaymentStatus)
                .statusChangeDateTime(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                .booking(booking)
                .build());

        return bookingRepo.save(booking);
    }

    @Override
    @CacheEvict(cacheNames = {"bookings", "bookings_paging"}, allEntries = true)
    public String delete(Long id) {
        Booking foundBooking = findById(id);
        PaymentStatus oldPaymentStatus = foundBooking.getPaymentStatus();
        if (oldPaymentStatus == PaymentStatus.CANCEL) {
            return "This Booking has already CANCELED";
        }
        foundBooking.setPaymentStatus(PaymentStatus.CANCEL);
        bookingRepo.save(foundBooking);
        paymentHistoryRepo.save(PaymentHistory
                .builder()
                .oldStatus(oldPaymentStatus)
                .newStatus(foundBooking.getPaymentStatus())
                .statusChangeDateTime(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                .booking(foundBooking)
                .build());
        return "Update Booking<%d> PAYMENT_STATUS(%s ---> %s)".formatted(id, oldPaymentStatus, PaymentStatus.CANCEL);
    }

    @Override
    public List<Booking> getAllBookingFromTripAndDate(Long tripId) {
        return bookingRepo.getAllBookingFromTripAndDate(tripId);
    }
}
