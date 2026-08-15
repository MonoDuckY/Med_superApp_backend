package com.yourproject.backend.dtos.requests;

import java.time.LocalDate;

import com.yourproject.backend.models.WorkSession;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModifyApprovedWorkScheduleRequest {
    @NotNull(message = "Work date is required.")
    private LocalDate workDate;

    @NotNull(message = "Session is required.")
    private WorkSession session;

    @NotBlank(message = "Clinic room ID is required.")
    @Size(max = 100, message = "Clinic room ID must not exceed 100 characters.")
    private String roomId;

    @Size(max = 500, message = "Note must not exceed 500 characters.")
    private String note;
}
