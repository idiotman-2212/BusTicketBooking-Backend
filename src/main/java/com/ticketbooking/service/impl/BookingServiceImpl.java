package com.ticketbooking.service.impl;

import com.ticketbooking.dto.BookingRequest;
import com.ticketbooking.dto.EmailMessage;
import com.ticketbooking.dto.PageResponse;
import com.ticketbooking.exception.ResourceNotFoundException;
import com.ticketbooking.model.Booking;
import com.ticketbooking.model.LoyaltyTransaction;
import com.ticketbooking.model.PaymentHistory;
import com.ticketbooking.model.User;
import com.ticketbooking.model.enumType.PaymentStatus;
import com.ticketbooking.repo.BookingRepo;
import com.ticketbooking.repo.LoyaltyTransactionRepo;
import com.ticketbooking.repo.PaymentHistoryRepo;
import com.ticketbooking.repo.UserRepo;
import com.ticketbooking.service.*;
import com.ticketbooking.validator.ObjectValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepo bookingRepo;

    private final PaymentHistoryRepo paymentHistoryRepo;

    private final ObjectValidator<Booking> objectValidator;

    private final UserRepo userRepo;
    private final LoyaltyTransactionRepo loyaltyTransactionRepo;

    private final SmsService smsService;

    private final NotificationService notificationService;

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

    //đặt vé site1 khách vãng đã đăng nhập vào hệ thống
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"bookings", "bookings_paging"}, allEntries = true)
    public List<Booking> saveForRegisteredUser(BookingRequest bookingRequest) {

        String[] selectSeats = bookingRequest.getSeatNumber();

        // Tính tổng tiền phải trả cho mỗi ghế (chia đều theo số lượng ghế)
        BigDecimal totalPaymentPerSeat = bookingRequest.getTotalPayment()
                .divide(BigDecimal.valueOf(selectSeats.length), RoundingMode.HALF_UP);


        // Lấy thông tin người dùng để xử lý điểm thưởng
        User user = userRepo.findByUsername(bookingRequest.getUser().getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Booking> orderedBookings = new ArrayList<>();
        for (String seat : selectSeats) {

            // Áp dụng điểm thưởng nếu được yêu cầu
            BigDecimal pointsToApply = bookingRequest.getPointsToUse();
            if (pointsToApply != null && pointsToApply.compareTo(BigDecimal.ZERO) > 0 && user.hasEnoughPoints(pointsToApply)) {
                totalPaymentPerSeat = totalPaymentPerSeat.subtract(pointsToApply);
                user.deductLoyaltyPoints(pointsToApply);
            }

            orderedBookings.add(Booking
                    .builder()
                    .user(user)
                    .trip(bookingRequest.getTrip())
                    .bookingDateTime(bookingRequest.getBookingDateTime())
                    .seatNumber(seat)
                    .bookingType(bookingRequest.getBookingType())
                    .pickUpAddress(bookingRequest.getPickUpAddress())
                    .custFirstName(bookingRequest.getFirstName())
                    .custLastName(bookingRequest.getLastName())
                    .phone(bookingRequest.getPhone())
                    .email(bookingRequest.getEmail())
                    .totalPayment(totalPaymentPerSeat)
                    .paymentDateTime(LocalDateTime.now())
                    .paymentMethod(bookingRequest.getPaymentMethod())
                    .paymentStatus(bookingRequest.getPaymentStatus())
                    .pointsEarned(BigDecimal.ZERO) // Không tích điểm khi đặt vé
                    .pointsUsed(pointsToApply)
                    .build());
        }
        var savedBookings = bookingRepo.saveAll(orderedBookings);

        // Sau khi chuyến đi hoàn thành, cập nhật điểm thưởng cho từng booking
        for (Booking savedBooking : savedBookings) {
            if (bookingRequest.getTrip().getCompleted()) { // Kiểm tra nếu chuyến đi đã hoàn thành
                BigDecimal pointsEarned = savedBooking.getTotalPayment().multiply(new BigDecimal("0.01")).setScale(0, RoundingMode.DOWN);
                savedBooking.setPointsEarned(pointsEarned);
                user.addLoyaltyPoints(pointsEarned);

                // Lưu lịch sử giao dịch tích lũy điểm
                LoyaltyTransaction earnTransaction = LoyaltyTransaction.builder()
                        .user(user)
                        .booking(savedBooking)
                        .amount(pointsEarned)
                        .transactionDate(LocalDateTime.now())
                        .transactionType(LoyaltyTransaction.TransactionType.EARN)
                        .build();
                loyaltyTransactionRepo.save(earnTransaction);
            }

            // Lưu lịch sử giao dịch điểm sử dụng nếu có
            if (savedBooking.getPointsUsed() != null && savedBooking.getPointsUsed().compareTo(BigDecimal.ZERO) > 0) {
                LoyaltyTransaction useTransaction = LoyaltyTransaction.builder()
                        .user(user)
                        .booking(savedBooking)
                        .amount(savedBooking.getPointsUsed().negate()) // giá trị âm cho điểm sử dụng
                        .transactionDate(LocalDateTime.now())
                        .transactionType(LoyaltyTransaction.TransactionType.USE)
                        .build();
                loyaltyTransactionRepo.save(useTransaction);
            }
        }
        userRepo.save(user); // Lưu người dùng đã được cập nhật điểm thưởng


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
/*
        // Gửi SMS xác nhận vé đặt thành công
        String source = bookingRequest.getTrip().getSource().getName();// Ví dụ thông tin chuyến đi
        String destination = bookingRequest.getTrip().getDestination().getName();
        String busInfo = bookingRequest.getTrip().getCoach().getName(); // Ví dụ thông tin xe
        String departureTime = bookingRequest.getTrip().getDepartureDateTime().toString(); // Ngày giờ đi
        String seatNumbers = String.join(", ", bookingRequest.getSeatNumber());  // Danh sách ghế
        //BigDecimal totalPayment = bookingRequest.getTotalPayment();  // Tổng giá vé

        // Gọi service để gửi SMS và email
        notificationService.sendSmsConfirmation(
                bookingRequest.getPhone(), source, destination, busInfo, departureTime, seatNumbers, totalPaymentPerSeat
        );
        notificationService.sendEmailConfirmation(
                bookingRequest.getEmail(), source, destination, busInfo, departureTime, seatNumbers, totalPaymentPerSeat
        );
*/
        return savedBookings;
    }

    //đặt vé site2 khách vãng lai
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"bookings", "bookings_paging"}, allEntries = true)
    public List<Booking> saveForWalkInCustomer(BookingRequest bookingRequest) {
        String[] selectSeats = bookingRequest.getSeatNumber();
        BigDecimal totalPaymentPerSeat = bookingRequest.getTotalPayment().divide(BigDecimal.valueOf(selectSeats.length), RoundingMode.HALF_UP);

        List<Booking> orderedBookings = new ArrayList<>();

        for (String seat : selectSeats) {
            Booking booking = Booking.builder()
                    .trip(bookingRequest.getTrip())
                    .bookingDateTime(bookingRequest.getBookingDateTime())
                    .seatNumber(seat)
                    .custFirstName(bookingRequest.getFirstName())
                    .custLastName(bookingRequest.getLastName())
                    .phone(bookingRequest.getPhone())
                    .email(bookingRequest.getEmail())
                    .totalPayment(totalPaymentPerSeat)
                    .paymentDateTime(LocalDateTime.now())
                    .paymentMethod(bookingRequest.getPaymentMethod())
                    .paymentStatus(bookingRequest.getPaymentStatus())
                    .pointsEarned(BigDecimal.ZERO) // Không tích lũy điểm cho khách vãng lai
                    .pointsUsed(BigDecimal.ZERO) // Không sử dụng điểm cho khách vãng lai
                    .build();

            orderedBookings.add(booking);
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

        /*
        // Gửi SMS xác nhận vé đặt thành công
        String source = bookingRequest.getTrip().getSource().getName();// Ví dụ thông tin chuyến đi
        String destination = bookingRequest.getTrip().getDestination().getName();
        String busInfo = bookingRequest.getTrip().getCoach().getName(); // Ví dụ thông tin xe
        String departureTime = bookingRequest.getTrip().getDepartureDateTime().toString(); // Ngày giờ đi
        String seatNumbers = String.join(", ", bookingRequest.getSeatNumber());  // Danh sách ghế
        //BigDecimal totalPayment = bookingRequest.getTotalPayment();  // Tổng giá vé

        // Gọi service để gửi SMS và email
        notificationService.sendSmsConfirmation(
                bookingRequest.getPhone(), source, destination, busInfo, departureTime, seatNumbers, totalPaymentPerSeat
        );
        notificationService.sendEmailConfirmation(
                bookingRequest.getEmail(), source, destination, busInfo, departureTime, seatNumbers, totalPaymentPerSeat
        );
*/

        // Lưu booking
        return savedBookings;
    }

    private BigDecimal calculatePointsEarned(BigDecimal totalPayment) {
        // Logic to earn 0.5% of total payment as points
        BigDecimal pointsRate = new BigDecimal("0.01"); // 1%
        return totalPayment.multiply(pointsRate).setScale(0, RoundingMode.DOWN);
    }


    private BigDecimal applyPoints(BigDecimal totalPayment, BigDecimal pointsToUse, User user) {
        if (pointsToUse != null && user.hasEnoughPoints(pointsToUse)) {
            return totalPayment.subtract(pointsToUse);
        }
        return totalPayment;
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

        // Cập nhật booking
        Booking updatedBooking = bookingRepo.save(booking);

 /*
        // Gửi SMS xác nhận vé đặt thành công
        String source = booking.getTrip().getSource().getName();// Ví dụ thông tin chuyến đi
        String destination = booking.getTrip().getDestination().getName();
        String busInfo = booking.getTrip().getCoach().getName(); // Ví dụ thông tin xe
        String departureTime = booking.getTrip().getDepartureDateTime().toString(); // Ngày giờ đi
        String seatNumbers = String.join(", ", booking.getSeatNumber());  // Danh sách ghế
        BigDecimal totalPayment = booking.getTotalPayment();  // Tổng giá vé

        // Gọi service để gửi SMS và email
        notificationService.sendSmsConfirmation(
                booking.getPhone(), source, destination, busInfo, departureTime, seatNumbers, totalPayment
        );
        notificationService.sendEmailConfirmation(
                booking.getEmail(), source, destination, busInfo, departureTime, seatNumbers, totalPayment
        );
*/

        return updatedBooking;
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

    @Override
    public List<Booking> findBookingsByPhone(String phone) {
        return bookingRepo.findByPhone(phone);
    }
}