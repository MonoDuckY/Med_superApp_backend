package com.yourproject.backend.dtos.responses;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class APIResponse<T> {
    private String message;
    private T data;
}
