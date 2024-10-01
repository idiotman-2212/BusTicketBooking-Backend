package com.ticketbooking.repo;

import com.ticketbooking.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepo extends JpaRepository<Notification, Long> {
    List<Notification> findByUser_UsernameOrderByCreatedAtDesc(String username);

    @Query("SELECT n FROM Notification n WHERE n.user.username = :username AND n.createdAt >= :startDate")
    List<Notification> findRecentNotifications(@Param("username") String username, @Param("startDate") LocalDateTime startDate);

    List<Notification> findByUser_UsernameAndIsReadFalse(String username);
}
