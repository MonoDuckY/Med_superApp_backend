package com.yourproject.backend.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "news")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class News {
    @Id
    private String newsId;
    private String title;
    private String content;
    @Builder.Default
    private List<String> image = new ArrayList<>();
    private String coverPhoto;
    private String uploadBy;
    private NewsStatus status;
    private Instant uploadTime;
}
