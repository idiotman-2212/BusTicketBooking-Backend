package com.ticketbooking.service.impl;

import com.ticketbooking.dto.BookingRequest;
import com.ticketbooking.dto.CargoRequest;
import com.ticketbooking.dto.PageResponse;
import com.ticketbooking.exception.ResourceNotFoundException;
import com.ticketbooking.model.*;
import com.ticketbooking.model.enumType.PaymentStatus;
import com.ticketbooking.model.enumType.TransactionType;
import com.ticketbooking.repo.*;
import com.ticketbooking.service.BookingService;
import com.ticketbooking.service.NotificationService;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private final CargoRepo cargoRepo;
    private final BookingCargoRepo bookingCargoRepo;


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
    @Transactional
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

    //đặt vé site1 khách đã đăng nhập vào hệ thống
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"bookings", "bookings_paging"}, allEntries = true)
    public List<Booking> saveForRegisteredUser(BookingRequest bookingRequest) {
        String[] selectSeats = bookingRequest.getSeatNumber();
        BigDecimal originalTotalPayment = bookingRequest.getTotalPayment();
        BigDecimal pointsUsed = bookingRequest.getPointsUsed();

        User user = userRepo.findByUsername(bookingRequest.getUser().getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BigDecimal totalPaymentPerSeat = originalTotalPayment.divide(BigDecimal.valueOf(selectSeats.length), RoundingMode.HALF_UP);

        List<Booking> orderedBookings = new ArrayList<>();
        for (String seat : selectSeats) {
            Booking booking = Booking.builder()
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
                    .pointsEarned(BigDecimal.ZERO)
                    .pointsUsed(pointsUsed.divide(BigDecimal.valueOf(selectSeats.length), RoundingMode.HALF_UP))
                    .build();

            // Thêm BookingCargo vào Booking trước khi lưu
            List<BookingCargo> bookingCargos = new ArrayList<>();
            BigDecimal totalCargoPrice = BigDecimal.ZERO;
            for (CargoRequest cargoRequest : bookingRequest.getCargoRequests()) {
                Cargo cargo = cargoRepo.findById(cargoRequest.getCargoId())
                        .orElseThrow(() -> new ResourceNotFoundException("Cargo not found"));
                BigDecimal cargoPrice = cargo.getBasePrice().multiply(new BigDecimal(cargoRequest.getQuantity()));
                totalCargoPrice = totalCargoPrice.add(cargoPrice);

                BookingCargo bookingCargo = BookingCargo.builder()
                        .booking(booking)
                        .cargo(cargo)
                        .quantity(cargoRequest.getQuantity())
                        .price(cargoPrice)
                        .build();
                bookingCargos.add(bookingCargo);
            }
            booking.setBookingCargos(bookingCargos);
            booking.setTotalPayment(booking.getTotalPayment().add(totalCargoPrice));
            orderedBookings.add(booking);
        }

        var savedBookings = bookingRepo.saveAll(orderedBookings);
        bookingRepo.flush(); // Lưu ngay lập tức để đảm bảo dữ liệu được lưu vào cơ sở dữ liệu

        // Xử lý điểm thưởng cho người dùng
        if (pointsUsed.compareTo(BigDecimal.ZERO) > 0) {
            user.deductLoyaltyPoints(pointsUsed);
            userRepo.save(user);

            // Lưu lịch sử giao dịch điểm sử dụng
            LoyaltyTransaction useTransaction = LoyaltyTransaction.builder()
                    .user(user)
                    .booking(savedBookings.get(0))
                    .amount(pointsUsed.negate())
                    .transactionDate(LocalDateTime.now())
                    .transactionType(TransactionType.USE)
                    .build();
            loyaltyTransactionRepo.save(useTransaction);
        }

        // Nạp lại Booking từ cơ sở dữ liệu để đảm bảo `bookingCargos` đã được nạp đầy đủ
        List<Booking> updatedBookings = savedBookings.stream()
                .map(booking -> bookingRepo.findById(booking.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Booking not found")))
                .collect(Collectors.toList());

        // Xử lý tích điểm sau khi chuyến đi hoàn thành
        if (bookingRequest.getTrip().getCompleted()) {
            for (Booking savedBooking : updatedBookings) {
                BigDecimal pointsEarned = savedBooking.getTotalPayment().multiply(new BigDecimal("0.01")).setScale(0, RoundingMode.DOWN);
                savedBooking.setPointsEarned(pointsEarned);
                user.addLoyaltyPoints(pointsEarned);

                // Lưu lịch sử giao dịch tích lũy điểm
                LoyaltyTransaction earnTransaction = LoyaltyTransaction.builder()
                        .user(user)
                        .booking(savedBooking)
                        .amount(pointsEarned)
                        .transactionDate(LocalDateTime.now())
                        .transactionType(TransactionType.EARN)
                        .build();
                loyaltyTransactionRepo.save(earnTransaction);
            }
        }
        userRepo.save(user);

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
        return updatedBookings;
    }



    //đặt vé site2 khách vãng lai
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"bookings", "bookings_paging"}, allEntries = true)
    public List<Booking> saveForWalkInCustomer(BookingRequest bookingRequest) {
        String[] selectSeats = bookingRequest.getSeatNumber();
        BigDecimal totalPaymentPerSeat = bookingRequest.getTotalPayment()
                .divide(BigDecimal.valueOf(selectSeats.length), RoundingMode.HALF_UP);

        List<Booking> orderedBookings = new ArrayList<>();
        for (String seat : selectSeats) {
            Booking booking = Booking.builder()
                    .trip(bookingRequest.getTrip())
                    .bookingDateTime(bookingRequest.getBookingDateTime())
                    .seatNumber(seat)
                    .bookingType(bookingRequest.getBookingType())
                    .custFirstName(bookingRequest.getFirstName())
                    .custLastName(bookingRequest.getLastName())
                    .phone(bookingRequest.getPhone())
                    .email(bookingRequest.getEmail())
                    .pickUpAddress(bookingRequest.getPickUpAddress())
                    .totalPayment(totalPaymentPerSeat)
                    .paymentDateTime(LocalDateTime.now())
                    .paymentMethod(bookingRequest.getPaymentMethod())
                    .paymentStatus(bookingRequest.getPaymentStatus())
                    .pointsEarned(BigDecimal.ZERO)
                    .pointsUsed(BigDecimal.ZERO)
                    .build();

            // Thêm BookingCargo vào Booking trước khi lưu
            BigDecimal totalCargoPrice = BigDecimal.ZERO;
            List<BookingCargo> bookingCargos = new ArrayList<>();

            // Kiểm tra xem cargoRequests có khác null không
            if (bookingRequest.getCargoRequests() != null) {
                for (CargoRequest cargoRequest : bookingRequest.getCargoRequests()) {
                    Cargo cargo = cargoRepo.findById(cargoRequest.getCargoId())
                            .orElseThrow(() -> new ResourceNotFoundException("Cargo not found"));
                    BigDecimal cargoPrice = cargo.getBasePrice().multiply(new BigDecimal(cargoRequest.getQuantity()));
                    totalCargoPrice = totalCargoPrice.add(cargoPrice);

                    BookingCargo bookingCargo = BookingCargo.builder()
                            .booking(booking)
                            .cargo(cargo)
                            .quantity(cargoRequest.getQuantity())
                            .price(cargoPrice)
                            .build();
                    bookingCargos.add(bookingCargo);
                }
            }

            booking.setBookingCargos(bookingCargos);
            booking.setTotalPayment(booking.getTotalPayment().add(totalCargoPrice));
            orderedBookings.add(booking);
        }

        // Lưu các Booking với BookingCargo đi kèm ngay lập tức
        var savedBookings = bookingRepo.saveAll(orderedBookings);
        bookingRepo.flush(); // Đẩy dữ liệu ngay lập tức vào cơ sở dữ liệu

        // Nạp lại Booking từ database để đảm bảo tất cả BookingCargo đã được nạp đầy đủ
        List<Booking> updatedBookings = savedBookings.stream()
                .map(booking -> bookingRepo.findById(booking.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Booking not found")))
                .collect(Collectors.toList());

        // Lưu lịch sử thanh toán
        List<PaymentHistory> paymentHistories = new ArrayList<>();
        for (Booking savedBooking : updatedBookings) {
            paymentHistories.add(PaymentHistory
                    .builder()
                    .booking(savedBooking)
                    .oldStatus(null)
                    .newStatus(savedBooking.getPaymentStatus())
                    .statusChangeDateTime(savedBooking.getPaymentDateTime())
                    .build());
        }
        paymentHistoryRepo.saveAll(paymentHistories);

        System.out.println("Booking Request: " + bookingRequest);
        System.out.println("Cargo Requests: " + bookingRequest.getCargoRequests());

        return updatedBookings;
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