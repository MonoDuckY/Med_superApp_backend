package com.yourproject.backend.dtos.responses;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FeedbackResponse {
    String feedbackId;
    String senderId;
    String receiverId;
    String content;
    String status;
    Integer rating;
    String serviceType;
    String response;
}
