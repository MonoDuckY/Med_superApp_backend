package com.yourproject.backend.dtos.requests;

import java.time.LocalDate;

import com.yourproject.backend.models.WorkSession;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitWorkScheduleRequest {
    @NotNull(message = "Work date is required.")
    private LocalDate workDate;

    @NotNull(message = "Session is required.")
    private WorkSession session;

    @Size(max = 500, message = "Note must not exceed 500 characters.")
    private String note;
}
