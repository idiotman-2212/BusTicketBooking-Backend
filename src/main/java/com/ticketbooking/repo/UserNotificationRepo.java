package com.ticketbooking.repo;

import com.ticketbooking.model.Notification;
import com.ticketbooking.model.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserNotificationRepo extends JpaRepository<UserNotification, Long> {
    List<UserNotification> findByUser_UsernameOrderByNotification_SendDateTimeDesc(String username);
    void deleteByNotificationId(Long notificationId);
    void deleteAll();

}
