package com.yourproject.backend.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "datasets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dataset {
    @Id
    private String id;
    
    private String name;
    private String description;
    
    private String status; 
    
    private String researcherId;
    
    private String rawZipUrl;
    private String processedZipUrl;
    
    private int imageCount;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
