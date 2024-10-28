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
import java.util.List;
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

        // Tính tổng tiền dịch vụ vận chuyển
        BigDecimal totalCargoPrice = BigDecimal.ZERO;
        for (CargoRequest cargoRequest : bookingRequest.getCargoRequests()) {
            Cargo cargo = cargoRepo.findById(cargoRequest.getCargoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cargo not found"));
            BigDecimal cargoPrice = cargo.getBasePrice().multiply(new BigDecimal(cargoRequest.getQuantity()));
            totalCargoPrice = totalCargoPrice.add(cargoPrice);
        }

        // Tính giá vé cơ bản (không bao gồm cargo và points)
        BigDecimal baseTicketPrice = originalTotalPayment.subtract(totalCargoPrice).add(pointsUsed);
        BigDecimal baseTicketPricePerSeat = baseTicketPrice.divide(BigDecimal.valueOf(selectSeats.length), RoundingMode.HALF_UP);

        // Chia đều tiền vận chuyển và điểm xu cho mỗi ghế
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

            // Thêm dịch vụ vận chuyển vào booking
            List<BookingCargo> bookingCargos = new ArrayList<>();
            for (CargoRequest cargoRequest : bookingRequest.getCargoRequests()) {
                Cargo cargo = cargoRepo.findById(cargoRequest.getCargoId())
                        .orElseThrow(() -> new ResourceNotFoundException("Cargo not found"));
                BigDecimal cargoPrice = cargo.getBasePrice().multiply(new BigDecimal(cargoRequest.getQuantity()));
                BigDecimal cargoPriceForThisSeat = cargoPrice.divide(BigDecimal.valueOf(selectSeats.length), RoundingMode.HALF_UP);

                BookingCargo bookingCargo = BookingCargo.builder()
                        .booking(booking)
                        .cargo(cargo)
                        .quantity(cargoRequest.getQuantity())
                        .price(cargoPriceForThisSeat)
                        .build();
                bookingCargos.add(bookingCargo);
            }
            booking.setBookingCargos(bookingCargos);

            // Tính tổng tiền cuối cùng cho mỗi ghế
            BigDecimal finalTotalPayment = baseTicketPricePerSeat
                    .add(cargoPricePerSeat)
                    .subtract(pointsUsedPerSeat);

            booking.setTotalPayment(finalTotalPayment);

            orderedBookings.add(booking);
        }

        // Lưu các booking vào cơ sở dữ liệu
        var savedBookings = bookingRepo.saveAll(orderedBookings);
        bookingRepo.flush(); // Đảm bảo lưu ngay lập tức vào cơ sở dữ liệu

        // Lưu lịch sử thanh toán
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
        paymentHistoryRepo.saveAll(paymentHistories); // Lưu toàn bộ lịch sử thanh toán

        // Trừ điểm xu cho người dùng
        if (pointsUsed.compareTo(BigDecimal.ZERO) > 0) {
            user.deductLoyaltyPoints(pointsUsed); // Trừ điểm xu của user
            userRepo.save(user);

            // Lưu lịch sử giao dịch điểm xu
            LoyaltyTransaction useTransaction = LoyaltyTransaction.builder()
                    .user(user)
                    .booking(savedBookings.get(0))
                    .amount(pointsUsed.negate()) // Điểm trừ
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
        // Kiểm tra dữ liệu đầu vào
        if (bookingRequest.getTrip() == null || bookingRequest.getSeatNumber() == null) {
            throw new BookingException("Dữ liệu không hợp lệ.");
        }
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