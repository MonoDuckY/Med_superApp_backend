package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import com.yourproject.backend.models.NewsStatus;

@Data
public class NewsRequest {
    @NotBlank(message = "Title is required.")
    @Size(max = 150, message = "Title must not exceed 150 characters.")
    private String title;

    @NotBlank(message = "Content is required.")
    @Size(max = 10000, message = "Content must not exceed 10000 characters.")
    private String content;

    private NewsStatus status = NewsStatus.DRAFT;

}
