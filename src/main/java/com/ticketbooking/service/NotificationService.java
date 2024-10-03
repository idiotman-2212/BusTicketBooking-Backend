package com.ticketbooking.service;

import com.ticketbooking.dto.NotificationDTO;
import com.ticketbooking.dto.NotificationRequest;
import com.ticketbooking.dto.PageResponse;
import com.ticketbooking.model.Notification;
import com.ticketbooking.model.User;
import com.ticketbooking.model.UserNotification;

import java.math.BigDecimal;
import java.util.List;

public interface NotificationService {
    void sendSmsConfirmation(String phoneNumber, String source, String destination, String busInfo, String departureTime, String seatNumbers, BigDecimal totalPayment);

    void sendEmailConfirmation(String email, String source, String destination, String busInfo, String departureTime, String seatNumbers, BigDecimal totalPayment);

    List<Notification> findAll();

    Notification findById(Long id);

    PageResponse<NotificationDTO> findAll(Integer page, Integer limit);

    void sendTripCompletionNotification(Long tripId);
    void sendNotification(NotificationRequest request);
    List<UserNotification> getUserNotifications(String username);
    void markAsRead(Long notificationId, String username);
    void updateNotification(Long notificationId, NotificationRequest request);

    void deleteAllNotifications();
    void deleteNotificationById(Long notificationId);

}
