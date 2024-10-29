package com.ticketbooking.service.impl;

import com.ticketbooking.dto.BookingRequest;
import com.ticketbooking.dto.CargoRequest;
import com.ticketbooking.dto.PageResponse;
import com.ticketbooking.exception.BookingException;
import com.ticketbooking.exception.ResourceNotFoundException;
import com.ticketbooking.model.*;
import com.ticketbooking.model.enumType.PaymentMethod;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepo bookingRepo;
    private final TripRepo tripRepo;
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
    @Transactional
    public Booking getBookingWithCargos(Long bookingId) {
        return bookingRepo.findByIdWithCargos(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
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

        // Calculate total cargo price once
        BigDecimal totalCargoPrice = BigDecimal.ZERO;
        Map<Long, BigDecimal> cargoPrices = new HashMap<>();

        for (CargoRequest cargoRequest : bookingRequest.getCargoRequests()) {
            Cargo cargo = cargoRepo.findById(cargoRequest.getCargoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cargo not found"));
            BigDecimal cargoPrice = cargo.getBasePrice().multiply(new BigDecimal(cargoRequest.getQuantity()));
            totalCargoPrice = totalCargoPrice.add(cargoPrice);
            cargoPrices.put(cargo.getId(), cargoPrice);
        }

        BigDecimal baseTicketPrice = originalTotalPayment.subtract(totalCargoPrice).add(pointsUsed);
        BigDecimal baseTicketPricePerSeat = baseTicketPrice.divide(BigDecimal.valueOf(selectSeats.length), RoundingMode.HALF_UP);
        BigDecimal cargoPricePerSeat = totalCargoPrice.divide(BigDecimal.valueOf(selectSeats.length), RoundingMode.HALF_UP);
        BigDecimal pointsUsedPerSeat = pointsUsed.divide(BigDecimal.valueOf(selectSeats.length), RoundingMode.HALF_UP);

        List<Booking> orderedBookings = new ArrayList<>();
        for (String seat : selectSeats) {
            Booking booking = Booking.builder()
                    .user(user)
                    .trip(bookingRequest.getTrip())
                    .bookingDateTime(bookingRequest.getBookingDateTime())
                    .seatNumber(seat)
                    .bookingType(bookingRequest.getBookingType())
                    .custFirstName(bookingRequest.getFirstName())
                    .custLastName(bookingRequest.getLastName())
                    .phone(bookingRequest.getPhone())
                    .email(bookingRequest.getEmail())
                    .paymentDateTime(LocalDateTime.now())
                    .paymentMethod(bookingRequest.getPaymentMethod())
                    .paymentStatus(bookingRequest.getPaymentStatus())
                    .pointsEarned(BigDecimal.ZERO)
                    .pointsUsed(pointsUsedPerSeat)
                    .build();

            // Add BookingCargo only for the first seat
            if (selectSeats[0].equals(seat)) {
                List<BookingCargo> bookingCargos = new ArrayList<>();
                for (CargoRequest cargoRequest : bookingRequest.getCargoRequests()) {
                    Cargo cargo = cargoRepo.findById(cargoRequest.getCargoId())
                            .orElseThrow(() -> new ResourceNotFoundException("Cargo not found"));

                    BookingCargo bookingCargo = BookingCargo.builder()
                            .booking(booking)
                            .cargo(cargo)
                            .quantity(cargoRequest.getQuantity())
                            .price(cargoPrices.get(cargo.getId()))
                            .build();
                    bookingCargos.add(bookingCargo);
                }
                booking.setBookingCargos(bookingCargos);
            }

            BigDecimal finalTotalPayment = baseTicketPricePerSeat
                    .add(cargoPricePerSeat)
                    .subtract(pointsUsedPerSeat);

            booking.setTotalPayment(finalTotalPayment);
            orderedBookings.add(booking);
        }

        var savedBookings = bookingRepo.saveAll(orderedBookings);
        bookingRepo.flush();

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

        if (pointsUsed.compareTo(BigDecimal.ZERO) > 0) {
            user.deductLoyaltyPoints(pointsUsed);
            userRepo.save(user);

            LoyaltyTransaction useTransaction = LoyaltyTransaction.builder()
                    .user(user)
                    .booking(savedBookings.get(0))
                    .amount(pointsUsed.negate())
                    .transactionDate(LocalDateTime.now())
                    .transactionType(TransactionType.USE)
                    .build();
            loyaltyTransactionRepo.save(useTransaction);
        }

        return savedBookings;
    }


    //đặt vé site2 khách vãng lai
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"bookings", "bookings_paging"}, allEntries = true)
    public List<Booking> saveForWalkInCustomer(BookingRequest bookingRequest) {
        if (bookingRequest.getTrip() == null || bookingRequest.getSeatNumber() == null) {
            throw new BookingException("Dữ liệu không hợp lệ.");
        }
        String[] selectSeats = bookingRequest.getSeatNumber();

        // Calculate total cargo price
        BigDecimal totalCargoPrice = BigDecimal.ZERO;
        Map<Long, BigDecimal> cargoPrices = new HashMap<>();

        if (bookingRequest.getCargoRequests() != null) {
            for (CargoRequest cargoRequest : bookingRequest.getCargoRequests()) {
                Cargo cargo = cargoRepo.findById(cargoRequest.getCargoId())
                        .orElseThrow(() -> new ResourceNotFoundException("Cargo not found"));
                BigDecimal cargoPrice = cargo.getBasePrice().multiply(new BigDecimal(cargoRequest.getQuantity()));
                totalCargoPrice = totalCargoPrice.add(cargoPrice);
                cargoPrices.put(cargo.getId(), cargoPrice);
            }
        }

        // Calculate base ticket price per seat (không bao gồm cargo)
        BigDecimal baseTicketPrice = bookingRequest.getTotalPayment();
        BigDecimal ticketPricePerSeat = baseTicketPrice.divide(BigDecimal.valueOf(selectSeats.length), RoundingMode.HALF_UP);

        // Calculate cargo price per seat
        BigDecimal cargoPricePerSeat = totalCargoPrice.divide(BigDecimal.valueOf(selectSeats.length), RoundingMode.HALF_UP);

        List<Booking> orderedBookings = new ArrayList<>();
        for (String seat : selectSeats) {
            // Calculate total payment for this seat including ticket price and cargo share
            BigDecimal seatTotalPayment = ticketPricePerSeat.add(cargoPricePerSeat);

            Booking booking = Booking.builder()
                    .trip(bookingRequest.getTrip())
                    .bookingDateTime(bookingRequest.getBookingDateTime())
                    .seatNumber(seat)
                    .bookingType(bookingRequest.getBookingType())
                    .custFirstName(bookingRequest.getFirstName())
                    .custLastName(bookingRequest.getLastName())
                    .phone(bookingRequest.getPhone())
                    .email(bookingRequest.getEmail())
                    .totalPayment(seatTotalPayment)  // Set total payment including both ticket and cargo share
                    .paymentDateTime(LocalDateTime.now())
                    .paymentMethod(bookingRequest.getPaymentMethod())
                    .paymentStatus(bookingRequest.getPaymentStatus())
                    .pointsEarned(BigDecimal.ZERO)
                    .pointsUsed(BigDecimal.ZERO)
                    .build();

            // Add BookingCargo only for the first seat
            if (bookingRequest.getCargoRequests() != null && selectSeats[0].equals(seat)) {
                List<BookingCargo> bookingCargos = new ArrayList<>();
                for (CargoRequest cargoRequest : bookingRequest.getCargoRequests()) {
                    Cargo cargo = cargoRepo.findById(cargoRequest.getCargoId())
                            .orElseThrow(() -> new ResourceNotFoundException("Cargo not found"));

                    BookingCargo bookingCargo = BookingCargo.builder()
                            .booking(booking)
                            .cargo(cargo)
                            .quantity(cargoRequest.getQuantity())
                            .price(cargoPrices.get(cargo.getId()))
                            .build();
                    bookingCargos.add(bookingCargo);
                }
                booking.setBookingCargos(bookingCargos);
            }

            orderedBookings.add(booking);
        }

        var savedBookings = bookingRepo.saveAll(orderedBookings);
        bookingRepo.flush();

        // Save payment history
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
    @Transactional
    public String delete(Long id) {
        Booking foundBooking = findById(id);
        PaymentStatus oldPaymentStatus = foundBooking.getPaymentStatus();
        LocalDateTime departureTime = foundBooking.getTrip().getDepartureDateTime();
        LocalDateTime currentTime = LocalDateTime.now();

        // Kiểm tra nếu vé đã bị hủy hoặc hoàn tiền
        if (oldPaymentStatus == PaymentStatus.CANCEL || oldPaymentStatus == PaymentStatus.REFUNDED) {
            throw new BookingException("This Booking has already been CANCELED or REFUNDED");
        }

        // Kiểm tra thời gian hủy
        if (currentTime.isAfter(departureTime.minusHours(24))) {
            throw new BookingException("Booking <%d> cannot be canceled within 24 hours before departure.".formatted(id));
        }

        // Xử lý hủy vé
        String cancelResult = cancelBooking(foundBooking, oldPaymentStatus, currentTime);

        // Xử lý hoàn tiền nếu cần
        if (oldPaymentStatus == PaymentStatus.PAID && foundBooking.getPaymentMethod() == PaymentMethod.CARD) {
            return refundBooking(foundBooking, currentTime);
        }

        return cancelResult;
    }

    private String cancelBooking(Booking booking, PaymentStatus oldStatus, LocalDateTime currentTime) {
        booking.setPaymentStatus(PaymentStatus.CANCEL);
        bookingRepo.save(booking);

        paymentHistoryRepo.save(PaymentHistory.builder()
                .oldStatus(oldStatus)
                .newStatus(PaymentStatus.CANCEL)
                .statusChangeDateTime(currentTime)
                .booking(booking)
                .build());

        releaseSeat(booking.getSeatNumber());

        return "Booking <%d> has been canceled successfully.".formatted(booking.getId());
    }

    private String refundBooking(Booking booking, LocalDateTime currentTime) {
        boolean refundSuccess = simulateRefund(booking.getTotalPayment());
        if (!refundSuccess) {
            throw new RuntimeException("Refund failed.");
        }

        booking.setPaymentStatus(PaymentStatus.REFUNDED);
        bookingRepo.save(booking);

        paymentHistoryRepo.save(PaymentHistory.builder()
                .oldStatus(PaymentStatus.CANCEL)
                .newStatus(PaymentStatus.REFUNDED)
                .statusChangeDateTime(currentTime)
                .booking(booking)
                .build());

        // Đảm bảo ghế được giải phóng sau khi hoàn tiền
        releaseSeat(booking.getSeatNumber());

        return "Booking <%d> has been canceled and refunded successfully.".formatted(booking.getId());
    }

    private void releaseSeat(String seatNumber) {
        // Implement logic to release the seat
        // This might involve updating a seat status in a separate table
        // or sending a message to a seat management service
        System.out.println("Seat " + seatNumber + " has been released.");
    }

    private boolean simulateRefund(BigDecimal totalPayment) {
        // Implement actual refund logic here
        System.out.println("Refunding amount: " + totalPayment);
        return true; // Simulate successful refund
    }



    @Override
    public List<Booking> getAllBookingFromTripAndDate(Long tripId) {
        return bookingRepo.getAllBookingFromTripAndDate(tripId);
    }

    @Override
    public List<Booking> findBookingsByPhone(String phone) {
        return bookingRepo.findByPhone(phone);
    }

    @Override
    public List<String> getAvailableSeats(Long tripId) {
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        Coach coach = trip.getCoach();
        int capacity = coach.getCapacity();
        List<String> allSeats = generateSeats(capacity);

        List<Booking> bookings = bookingRepo.findAllByTripId(tripId);
        for (Booking booking : bookings) {
            String seatNumber = booking.getSeatNumber();
            if (seatNumber != null && allSeats.contains(seatNumber)
                    && booking.getPaymentStatus() != PaymentStatus.REFUNDED) {
                allSeats.remove(seatNumber);
            }
        }

        // Trả về danh sách ghế còn trống
        return allSeats;
    }


    // Hàm tạo danh sách ghế dựa trên sức chứa
    private List<String> generateSeats(int capacity) {
        List<String> seats = new ArrayList<>();
        int halfCapacity = (int) Math.ceil(capacity / 2.0);

        for (int i = 1; i <= halfCapacity; i++) {
            seats.add("A" + i);
        }
        for (int i = 1; i <= (capacity - halfCapacity); i++) {
            seats.add("B" + i);
        }

        return seats;
    }

}