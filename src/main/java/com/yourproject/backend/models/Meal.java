package com.yourproject.backend.models;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "meals")
@CompoundIndex(name = "user_meal_time_unique", def = "{'userId': 1, 'mealName': 1, 'scheduledAt': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Meal {
    @Id
    private String id;
    @Indexed
    private String userId;
    @Indexed
    private String prescriptionId;
    private String mealName;
    @Indexed
    private Instant scheduledAt;
    @Indexed
    private PlanScheduleStatus status;
    private String note;
}
