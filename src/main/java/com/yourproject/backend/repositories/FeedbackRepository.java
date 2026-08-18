package com.yourproject.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.yourproject.backend.models.Feedback;

public interface FeedbackRepository extends MongoRepository<Feedback, String> {
    List<Feedback> findAllByOrderByFeedbackIdDesc();

    List<Feedback> findAllByStatusOrderByFeedbackIdDesc(String status);

    List<Feedback> findAllBySenderIdOrderByFeedbackIdDesc(String senderId);

    Optional<Feedback> findByFeedbackIdAndSenderId(String feedbackId, String senderId);

}
