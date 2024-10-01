package com.ticketbooking.controller;

import com.ticketbooking.dto.NotificationDTO;
import com.ticketbooking.model.Notification;
import com.ticketbooking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications") // Đường dẫn chính
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // Lấy tất cả thông báo cho mục đích admin (nếu cần)
    @GetMapping("/all")
    public ResponseEntity<?> findAll(){
        return ResponseEntity.ok(notificationService.findAll());
    }

    // Lấy thông báo cho người dùng hiện tại
    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(Authentication authentication) {
        String username = authentication.getName();
        List<NotificationDTO> notifications = notificationService.getNotificationsForUser(username);
        System.out.println("Thông báo cho người dùng " + username + ": " + notifications);
        return ResponseEntity.ok(notifications);
    }

    // Lấy thông báo trong 7 ngày gần đây
    @GetMapping("/recent")
    public ResponseEntity<List<NotificationDTO>> getRecentNotifications(Authentication authentication) {
        String username = authentication.getName();
        List<NotificationDTO> notifications = notificationService.getRecentNotificationsForUser(username);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(Authentication authentication) {
        String username = authentication.getName();
        List<NotificationDTO> unreadNotifications = notificationService.getUnreadNotificationsForUser(username);
        return ResponseEntity.ok(unreadNotifications);
    }


    // Đánh dấu thông báo là đã đọc
    @PostMapping("/{id}/mark-as-read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        notificationService.markNotificationAsRead(id);
        return ResponseEntity.ok().build();
    }

    // Thêm thông báo cho người dùng
    @PostMapping
    public ResponseEntity<?> addNotification(@RequestParam String username, @RequestParam String title, @RequestParam String message) {
        notificationService.addNotificationForUser(username, title, message);
        return ResponseEntity.ok().build();
    }

    // Cập nhật thông báo
    @PutMapping("/{id}")
    public ResponseEntity<?> updateNotification(@PathVariable Long id, @RequestParam String message) {
        notificationService.updateNotification(id, message);
        return ResponseEntity.ok().build();
    }

    // Xóa thông báo
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok().build();
    }
}
