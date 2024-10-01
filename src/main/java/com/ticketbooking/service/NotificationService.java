package com.ticketbooking.service;

import com.ticketbooking.dto.NotificationDTO;
import com.ticketbooking.model.Notification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

public interface NotificationService {
    void sendSmsConfirmation(String phoneNumber, String source, String destination, String busInfo, String departureTime, String seatNumbers, BigDecimal totalPayment);
    void sendEmailConfirmation(String email, String source, String destination, String busInfo, String departureTime, String seatNumbers, BigDecimal totalPayment);

    List<Notification> findAll();
    void markNotificationAsRead(Long notificationId);
    List<NotificationDTO> getRecentNotificationsForUser(String username);
    List<NotificationDTO> getUnreadNotificationsForUser(String username);
    List<NotificationDTO> getNotificationsForUser(String username);
    Notification addNotificationForUser(String username,String title, String message);
    void deleteNotification(Long notificationId);
    void updateNotification(Long notificationId, String newMessage);
}
