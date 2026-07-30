package com.yourproject.backend.models;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "clinic_rooms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicRoom {
    @Id
    private String id;

    @Indexed(unique = true)
    private String code;

    private String name;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
