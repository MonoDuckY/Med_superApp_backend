package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BlockWorkSlotRequest {
    @NotBlank(message = "Block reason is required.")
    @Size(max = 500, message = "Block reason must not exceed 500 characters.")
    private String reason;
}
