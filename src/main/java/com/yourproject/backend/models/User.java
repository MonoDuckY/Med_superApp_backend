package com.yourproject.backend.models;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
}
