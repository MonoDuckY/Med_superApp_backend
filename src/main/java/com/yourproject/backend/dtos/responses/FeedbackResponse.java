package com.yourproject.backend.dtos.responses;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FeedbackResponse {
    String feedbackId;
    String senderId;
    String senderName;
    String receiverId;
    String receiverName;
    String content;
    String status;
    Integer rating;
    String serviceType;
    String response;
}
