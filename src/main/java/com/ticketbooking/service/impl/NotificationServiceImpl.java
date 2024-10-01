package com.ticketbooking.service.impl;

import com.ticketbooking.dto.EmailMessage;
import com.ticketbooking.dto.NotificationDTO;
import com.ticketbooking.exception.ResourceNotFoundException;
import com.ticketbooking.model.Notification;
import com.ticketbooking.model.Trip;
import com.ticketbooking.model.User;
import com.ticketbooking.repo.NotificationRepo;
import com.ticketbooking.repo.UserRepo;
import com.ticketbooking.service.MailService;
import com.ticketbooking.service.NotificationService;
import com.ticketbooking.service.SmsService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl  implements NotificationService {

    private final SmsService smsService;
    private final MailService mailService;
    private final Environment env;
    private final NotificationRepo notificationRepo;
    private final UserRepo userRepo;

    @Override
    public void sendSmsConfirmation(String phoneNumber, String source, String destination, String busInfo, String departureTime, String seatNumbers, BigDecimal totalPayment) {
        String message = String.format(
                "THÔNG TIN VÉ ĐẶT\n" +
                        "Tuyến: %s => %s\n" +
                        "Xe: %s\n" +
                        "Ngày đi: %s\n" +
                        "Ghế: %s\n" +
                        "Giá vé: %s",
                source, destination, busInfo, departureTime, seatNumbers,
                NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(totalPayment)
        );

        // Chuyển đổi số điện thoại
        if (phoneNumber != null && phoneNumber.startsWith("0")) {
            phoneNumber = "+84" + phoneNumber.substring(1);
        }

        // Gửi SMS tới số điện thoại khách hàng
        smsService.sendSms(phoneNumber, message);
    }

    @Override
    public void sendEmailConfirmation(String email, String source, String destination, String busInfo, String departureTime, String seatNumbers, BigDecimal totalPayment) {
        String emailContent = String.format(
                "Kính chào quý khách,\n\n" +
                        "Cảm ơn bạn đã đặt vé với chúng tôi. Dưới đây là thông tin chi tiết vé của bạn:\n\n" +
                        "Tuyến: %s => %s\n" +
                        "Xe: %s\n" +
                        "Ngày đi: %s\n" +
                        "Ghế: %s\n" +
                        "Giá vé: %s\n\n" +
                        "Chúng tôi hy vọng bạn sẽ có một chuyến đi vui vẻ.\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ hỗ trợ khách hàng.",
                source, destination, busInfo, departureTime, seatNumbers,
                NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(totalPayment)
        );

        // Tạo đối tượng EmailMessage
        EmailMessage emailMessage = EmailMessage.builder()
                .from(env.getProperty("spring.mail.username"))
                .to(email)
                .subject("Xác nhận đặt vé thành công")
                .text(emailContent)
                .build();

        // Gửi email qua MailService
        mailService.send(emailMessage);
    }

    @Override
    public List<Notification> findAll() {
        return notificationRepo.findAll();
    }

    @Override
    public List<NotificationDTO> getNotificationsForUser(String username) {
        List<Notification> notifications = notificationRepo.findByUser_UsernameOrderByCreatedAtDesc(username);

        return notifications.stream()
                .map(notification -> {
                    User user = notification.getUser();
                    Trip trip = notification.getTrip();

                    return new NotificationDTO(
                            notification.getId(),
                            notification.getTitle(),
                            notification.getMessage(),
                            notification.getCreatedAt(),
                            notification.isRead(),
                            user != null ? user.getUsername() : null,
                            user != null ? user.getEmail() : null,
                            user != null ? user.getFirstName() : null,
                            user != null ? user.getLastName() : null,
                            trip != null ? trip.getId() : null,
                            trip != null ? trip.getSource().getName() : null,
                            trip != null ? trip.getDestination().getName() : null,
                            trip != null ? trip.getDepartureDateTime() : null,
                            trip != null ? trip.getPrice() : null
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationDTO> getRecentNotificationsForUser(String username) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Notification> notifications = notificationRepo.findRecentNotifications(username, sevenDaysAgo);
        return notifications
                .stream()
                .map(notification -> {
                    User user = notification.getUser();
                    Trip trip = notification.getTrip();

                    return NotificationDTO.builder()
                            .id(notification.getId())
                            .title(notification.getTitle())
                            .message(notification.getMessage())
                            .createdAt(notification.getCreatedAt())
                            .isRead(notification.isRead())
                            .username(user != null ? user.getUsername() : null)
                            .email(user != null ? user.getEmail() : null)
                            .firstName(user != null ? user.getFirstName() : null)
                            .lastName(user != null ? user.getLastName() : null)
                            .tripId(trip != null ? trip.getId() : null)
                            .source(trip != null ? trip.getSource().getName() : null)
                            .destination(trip != null ? trip.getDestination().getName() : null)
                            .departureTime(trip != null ? trip.getDepartureDateTime() : null)
                            .price(trip != null ? trip.getPrice() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationDTO> getUnreadNotificationsForUser(String username) {
        List<Notification> unreadNotifications = notificationRepo.findByUser_UsernameAndIsReadFalse(username);

        return unreadNotifications.stream()
                .map(notification -> {
                    User user = notification.getUser();
                    Trip trip = notification.getTrip();

                    return new NotificationDTO(
                            notification.getId(),
                            notification.getTitle(),
                            notification.getMessage(),
                            notification.getCreatedAt(),
                            notification.isRead(),
                            user != null ? user.getUsername() : null,
                            user != null ? user.getEmail() : null,
                            user != null ? user.getFirstName() : null,
                            user != null ? user.getLastName() : null,
                            trip != null ? trip.getId() : null,
                            trip != null ? trip.getSource().getName() : null,
                            trip != null ? trip.getDestination().getName() : null,
                            trip != null ? trip.getDepartureDateTime() : null,
                            trip != null ? trip.getPrice() : null
                    );
                })
                .collect(Collectors.toList());
    }


    @Override
    public void markNotificationAsRead(Long notificationId) {
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepo.save(notification);
        }
    }

    @Override
    @Transactional
    public Notification addNotificationForUser(String username,String title, String message) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        LocalDateTime nowInVietnam = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        notification.setCreatedAt(nowInVietnam);
        notificationRepo.save(notification);
        return notification;
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepo.deleteById(notificationId);
    }

    @Override
    @Transactional
    public void updateNotification(Long notificationId, String newMessage) {
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setMessage(newMessage);
        notificationRepo.save(notification);
    }
}
