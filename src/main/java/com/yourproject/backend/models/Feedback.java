package com.yourproject.backend.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "feedback")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {
    @Id
    private String feedbackId;
    private String senderId;
    private String receiverId;
    private String content;
    private String status;
    private Integer rating;
    private String serviceType;
    private String response;
}
