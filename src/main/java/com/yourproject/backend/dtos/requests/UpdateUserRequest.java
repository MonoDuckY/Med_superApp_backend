package com.yourproject.backend.dtos.requests;

import java.time.LocalDate;
import java.util.Set;

import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.UserRole;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private Set<UserRole> roles;
    public UserRole getRole() { return roles == null || roles.isEmpty() ? null : roles.iterator().next(); }
    public void setRole(UserRole role) { roles = role == null ? null : Set.of(role); }
    private AccountStatus status;

    @Size(max = 100, message = "Full name must not exceed 100 characters.")
    private String fullName;

    @Size(max = 20, message = "Gender must not exceed 20 characters.")
    private String gender;

    private LocalDate dateOfBirth;

    @Size(max = 20, message = "Phone number must not exceed 20 characters.")
    private String phoneNumber;

    @Size(max = 255, message = "Address must not exceed 255 characters.")
    private String address;

    @Size(max = 50, message = "Citizen identification code must not exceed 50 characters.")
    private String citizenIdentificationCode;

    @Size(max = 50, message = "Health insurance code must not exceed 50 characters.")
    private String healthInsuranceCode;

    @Size(max = 255, message = "Certificate must not exceed 255 characters.")
    private String certificate;
    private String medicalHistory;
    private String currentSickness;
    private Double height;
    private Double weight;
    private String bloodType;
}
