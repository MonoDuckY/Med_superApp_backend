package com.yourproject.backend.models;

import java.time.LocalTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "work_slots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkSlot {
    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private LocalTime startTime;
    private LocalTime endTime;
    @Transient
    private WorkSession session;
    @Transient
    private boolean active;
}
