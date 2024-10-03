package com.ticketbooking.controller;

import com.ticketbooking.dto.NotificationDTO;
import com.ticketbooking.dto.NotificationRequest;
import com.ticketbooking.dto.PageResponse;
import com.ticketbooking.model.UserNotification;
import com.ticketbooking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/all")
    public ResponseEntity<?> findAll(){
        return ResponseEntity.ok(notificationService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id){
        return ResponseEntity.ok(notificationService.findById(id));
    }

    @GetMapping("/paging")
    public PageResponse<NotificationDTO> getPageOfDrivers(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "5") Integer limit) {
        return notificationService.findAll(page, limit);
    }

    // Lấy danh sách thông báo của người dùng
    @GetMapping
    public List<UserNotification> getUserNotifications(Authentication authentication) {
        String username = authentication.getName();
        return notificationService.getUserNotifications(username);
    }

    // Đánh dấu thông báo là đã đọc
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long notificationId, @RequestParam String username) {
        notificationService.markAsRead(notificationId, username);
        return ResponseEntity.ok().build();
    }

    // Admin gửi thông báo
    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@RequestBody NotificationRequest request) {
        notificationService.sendNotification(request);
        return ResponseEntity.ok("Notification sent successfully.");
    }

    @PutMapping("/{notificationId}")
    public ResponseEntity<?> updateNotification(
            @PathVariable Long notificationId,
            @RequestBody NotificationRequest request) {
        notificationService.updateNotification(notificationId, request);
        return ResponseEntity.ok("Notification updated successfully.");
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<?> deleteNotificationById(@PathVariable Long notificationId) {
        notificationService.deleteNotificationById(notificationId);
        return ResponseEntity.ok("Notification deleted successfully.");
    }

    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAllNotifications() {
        notificationService.deleteAllNotifications();
        return ResponseEntity.ok("All notifications deleted successfully.");
    }

}
