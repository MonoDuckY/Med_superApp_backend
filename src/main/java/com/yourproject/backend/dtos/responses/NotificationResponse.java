package com.yourproject.backend.dtos.responses;

import java.time.Instant;

import com.yourproject.backend.models.Notification;
import com.yourproject.backend.models.NotificationStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationResponse {
    private String id;
    private String content;
    private Instant notifyTime;
    private NotificationStatus status;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .content(notification.getContent())
                .notifyTime(notification.getNotifyTime())
                .status(notification.getStatus())
                .build();
    }
}
