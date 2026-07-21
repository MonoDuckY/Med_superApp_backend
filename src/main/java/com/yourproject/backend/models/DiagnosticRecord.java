package com.yourproject.backend.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document(collection = "diagnostic_records")
@Data
public class DiagnosticRecord {
    @Id
    private String id;
    private String details;
}
