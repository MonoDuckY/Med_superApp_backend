package com.yourproject.backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.dtos.responses.NotificationResponse;
import com.yourproject.backend.services.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/patient/notifications")
@PreAuthorize("hasRole('PATIENT')")
@RequiredArgsConstructor
public class PatientNotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Notifications retrieved successfully.",
                notificationService.getNotifications(authentication.getName())));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Unread notification count retrieved successfully.",
                notificationService.getUnreadCount(authentication.getName())));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            Authentication authentication,
            @PathVariable String notificationId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Notification marked as read.",
                notificationService.markRead(authentication.getName(), notificationId)));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(Authentication authentication) {
        notificationService.markAllRead(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read.", null));
    }
}
