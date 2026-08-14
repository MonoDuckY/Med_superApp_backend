package com.yourproject.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.yourproject.backend.models.Notification;
import com.yourproject.backend.models.NotificationStatus;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findAllByUserIdOrderByNotifyTimeDesc(String userId);

    Optional<Notification> findByIdAndUserId(String id, String userId);

    long countByUserIdAndStatus(String userId, NotificationStatus status);

    List<Notification> findAllByUserIdAndStatus(String userId, NotificationStatus status);
}
