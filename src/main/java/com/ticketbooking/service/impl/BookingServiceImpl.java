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
import com.ticketbooking.service.LoyaltyPointsService;
import com.ticketbooking.service.SmsService;
import com.ticketbooking.validator.ObjectValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    private final SmsService smsService;

    private final LoyaltyPointsService loyaltyPointsService;

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

        User user = bookingRequest.getUser();
        BigDecimal totalPayment = bookingRequest.getTotalPayment();
        BigDecimal pointsUsed = bookingRequest.getPointsUsed();
        BigDecimal discountAmount = bookingRequest.getDiscountAmount();

        // Kiểm tra và áp dụng xu
        if (pointsUsed != null && pointsUsed.compareTo(BigDecimal.ZERO) > 0) {
            if (pointsUsed.compareTo(user.getLoyaltyPoints()) > 0) {
                throw new IllegalArgumentException("Không đủ xu để sử dụng");
            }

            // Tính toán số tiền giảm giá (giả sử 1 xu = 1000 VND)
            discountAmount = pointsUsed.multiply(new BigDecimal("1000"));

            // Đảm bảo số tiền giảm giá không vượt quá tổng số tiền thanh toán
            if (discountAmount.compareTo(totalPayment) > 0) {
                discountAmount = totalPayment;
                pointsUsed = totalPayment.divide(new BigDecimal("1000"), 0, BigDecimal.ROUND_DOWN);
            }

            // Cập nhật tổng số tiền thanh toán
            totalPayment = totalPayment.subtract(discountAmount);

            // Cập nhật số xu của người dùng
            user.setLoyaltyPoints(user.getLoyaltyPoints().subtract(pointsUsed));
            userRepo.save(user);
        }


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
                    //.totalPayment(BigDecimal.valueOf(bookingRequest.getTotalPayment().longValue() / selectSeats.length))
                    .totalPayment(totalPayment)
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

        // Gửi SMS xác nhận vé đặt thành công
        String source = bookingRequest.getTrip().getSource().getName();// Ví dụ thông tin chuyến đi
        String destination = bookingRequest.getTrip().getDestination().getName();

        String busInfo = bookingRequest.getTrip().getCoach().getName(); // Ví dụ thông tin xe
        String departureTime = bookingRequest.getTrip().getDepartureDateTime().toString().formatted("yyyy-MM-dd HH:mm"); // Ngày giờ đi
        String seatNumbers = String.join(", ", bookingRequest.getSeatNumber());  // Danh sách ghế
        //BigDecimal totalPayment = bookingRequest.getTotalPayment();  // Tổng giá vé

        // Gửi SMS xác nhận vé đặt thành công
        String message = String.format(
                "THÔNG TIN VÉ ĐẶT\n" +
                        "Tuyến: " + source + " => " + destination + "\n" +
                        "Xe: " + busInfo + "\n" +
                        "Ngày đi: " + departureTime + "\n" +
                        "Ghế: " + seatNumbers + "\n" +
                        "Giá vé: " + NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(totalPayment)
        );

        // Chuyển đổi số điện thoại
        String phoneNumber = bookingRequest.getPhone();
        if (phoneNumber != null && phoneNumber.startsWith("0")) {
            phoneNumber = "+84" + phoneNumber.substring(1);
        }

        // Gửi SMS tới số điện thoại khách hàng
        smsService.sendSms(phoneNumber, message);

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

        // Cập nhật booking
        Booking updatedBooking = bookingRepo.save(booking);

        // Gửi SMS xác nhận cập nhật
        String source = updatedBooking.getTrip().getSource().getName();
        String destination = updatedBooking.getTrip().getDestination().getName();
        String busInfo = updatedBooking.getTrip().getCoach().getName();
        String departureTime = updatedBooking.getTrip().getDepartureDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String seatNumbers = updatedBooking.getSeatNumber();
        BigDecimal totalPayment = updatedBooking.getTotalPayment();

        String message = String.format(
                "THÔNG TIN VÉ ĐẶT\n" +
                        "Tuyến: " + source + " => " + destination + "\n" +
                        "Xe: " + busInfo + "\n" +
                        "Ngày đi: " + departureTime + "\n" +
                        "Ghế: " + seatNumbers + "\n" +
                        "Giá vé: " + NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(totalPayment)
        );

        // Chuyển đổi số điện thoại
        String phoneNumber = updatedBooking.getPhone();
        if (phoneNumber != null && phoneNumber.startsWith("0")) {
            phoneNumber = "+84" + phoneNumber.substring(1);
        }

        // Gửi SMS tới số điện thoại khách hàng
        smsService.sendSms(phoneNumber, message);

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