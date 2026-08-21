package com.yourproject.backend.dtos.responses;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NewsResponse {
    String newsId;
    String title;
    String content;
    String uploadBy;
    List<Attachment> image;
    Attachment coverPhoto;
    String status;
    Instant uploadTime;

    @Value
    @Builder
    public static class Attachment {
        String url;
        Instant expiresAt;
    }
}
