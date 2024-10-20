package com.ticketbooking.service;

import com.ticketbooking.dto.EmailMessage;
import com.ticketbooking.dto.NotificationRequest;
import com.ticketbooking.model.Booking;
import com.ticketbooking.model.PaymentHistory;
import com.ticketbooking.model.enumType.PaymentStatus;
import com.ticketbooking.model.enumType.RecipientType;
import com.ticketbooking.repo.BookingRepo;
import com.ticketbooking.repo.PaymentHistoryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleJobService {

    private final BookingRepo bookingRepo;
    private final NotificationService notificationService;
    private final SmsService smsService;
    private final MailService mailService;
    private final Environment env;
    private final PaymentHistoryRepo paymentHistoryRepo;

    // 1. Nhắc nhở thanh toán cho vé UNPAID trước 1 ngày khởi hành
    @Scheduled(cron = "0 0 9 * * ?") // Chạy mỗi ngày lúc 9 giờ sáng
    public void remindUnpaidBookings() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime reminderThreshold = currentTime.plusDays(1); // Nhắc nhở trước 1 ngày khởi hành

        List<Booking> unpaidBookings = bookingRepo.findUnpaidBookingsBefore(reminderThreshold);

        for (Booking booking : unpaidBookings) {
            // Gửi thông báo qua hệ thống
            notificationService.sendNotification(
                    NotificationRequest.builder()
                            .title("Nhắc nhở thanh toán")
                            .message("Vé đặt của quý khách chưa được thanh toán. Vui lòng thanh toán trước 24 giờ khởi hành.")
                            .recipientType(RecipientType.INDIVIDUAL)
                            .senderUsername("System") // Thêm giá trị senderUsername
                            .recipientIdentifiers(List.of(booking.getUser().getUsername()))
                            .build()
            );

            // Gửi SMS
            String message = "Vé đặt của quý khách chưa được thanh toán. Vui lòng thanh toán trước 24 giờ khởi hành.";
            smsService.sendSms(booking.getPhone(), message);

            // Gửi Email
            String emailContent = "Kính chào quý khách, vui lòng thanh toán vé của bạn trước 24 giờ khởi hành.";
            mailService.send(new EmailMessage(env.getProperty("spring.mail.username"),
                    booking.getEmail(),
                    "Nhắc nhở thanh toán",
                    emailContent));
        }
    }

    // 2. Tự động hủy vé chưa thanh toán trước khi khởi hành
    @Scheduled(cron = "0 0 * * * ?") // Chạy mỗi giờ
    public void autoCancelUnpaidBookings() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime cancellationThreshold = currentTime.plusHours(24); // Hủy nếu chưa thanh toán và còn cách giờ khởi hành 24 giờ

        List<Booking> unpaidBookings = bookingRepo.findUnpaidBookingsBefore(cancellationThreshold);

        for (Booking booking : unpaidBookings) {
            // Thay đổi trạng thái của booking thành CANCEL
            PaymentStatus oldStatus = booking.getPaymentStatus();
            booking.setPaymentStatus(PaymentStatus.CANCEL);
            bookingRepo.save(booking);

            // Ghi lại lịch sử thanh toán
            paymentHistoryRepo.save(PaymentHistory.builder()
                    .oldStatus(oldStatus)
                    .newStatus(PaymentStatus.CANCEL)
                    .statusChangeDateTime(LocalDateTime.now())
                    .booking(booking)
                    .build());

            // Gửi thông báo hủy qua hệ thống
            notificationService.sendNotification(
                    NotificationRequest.builder()
                            .title("Vé đặt đã bị hủy")
                            .message("Vé của quý khách đã bị hủy do không thanh toán trước 24 giờ khởi hành.")
                            .recipientType(RecipientType.INDIVIDUAL)
                            .senderUsername("System") // Thêm giá trị senderUsername
                            .recipientIdentifiers(List.of(booking.getUser().getUsername()))
                            .build()
            );

            // Gửi SMS
            String message = "Vé của quý khách đã bị hủy do không thanh toán trước 24 giờ khởi hành.";
            smsService.sendSms(booking.getPhone(), message);

            // Gửi Email
            String emailContent = "Kính chào quý khách, vé của bạn đã bị hủy vì không thanh toán trước giờ khởi hành.";
            mailService.send(new EmailMessage(env.getProperty("spring.mail.username"),
                    booking.getEmail(),
                    "Vé đặt đã bị hủy",
                    emailContent));
        }
    }

    // Lên lịch gửi thông báo nhắc nhở thời gian khởi hành chuyến
    @Scheduled(cron = "0 0 9 * * ?")
    public void remindPaidBookingsForDeparture() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime reminderThreshold = currentTime.plusDays(1);  // Nhắc nhở trước 1 ngày khởi hành

        List<Booking> paidBookings = bookingRepo.findPaidBookingsBefore(reminderThreshold);

        for (Booking booking : paidBookings) {
            // Gửi thông báo qua hệ thống
            notificationService.sendNotification(
                    NotificationRequest.builder()
                            .title("Nhắc nhở khởi hành")
                            .message("Chuyến xe của quý khách sẽ khởi hành vào " + booking.getTrip().getDepartureDateTime())
                            .recipientType(RecipientType.INDIVIDUAL)
                            .senderUsername("System")  // Người gửi là hệ thống
                            .recipientIdentifiers(List.of(booking.getUser().getUsername()))
                            .build()
            );

            // Gửi SMS
            String message = "Chuyến xe của quý khách sẽ khởi hành vào " + booking.getTrip().getDepartureDateTime() + ". Vui lòng có mặt đúng giờ.";
            smsService.sendSms(booking.getPhone(), message);

            // Gửi Email
            String emailContent = "Kính chào quý khách, chuyến xe của bạn sẽ khởi hành vào " + booking.getTrip().getDepartureDateTime() + ". Vui lòng có mặt đúng giờ.";
            mailService.send(new EmailMessage(env.getProperty("spring.mail.username"),
                    booking.getEmail(),
                    "Nhắc nhở khởi hành",
                    emailContent));
        }
    }

}
