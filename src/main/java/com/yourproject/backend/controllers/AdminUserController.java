package com.yourproject.backend.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yourproject.backend.dtos.requests.CreateUserRequest;
import com.yourproject.backend.dtos.requests.UpdateUserRequest;
import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.dtos.responses.UserResponse;
import com.yourproject.backend.services.UserService;
import com.yourproject.backend.services.PatientDataProtectionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService userService;
    private final PatientDataProtectionService patientDataProtectionService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            Authentication authentication,
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse user = UserResponse.from(userService.createUser(request, authentication.getName()), patientDataProtectionService);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("User account created successfully.", user));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers() {
        List<UserResponse> users = userService.getAllUsers().stream().map(user -> UserResponse.from(user, patientDataProtectionService)).toList();
        return ResponseEntity.ok(ApiResponse.success("User accounts retrieved successfully.", users));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable String userId) {
        UserResponse user = UserResponse.from(userService.getUserById(userId), patientDataProtectionService);
        return ResponseEntity.ok(ApiResponse.success("User account retrieved successfully.", user));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            Authentication authentication,
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse user = UserResponse.from(userService.updateUser(userId, request, authentication.getName()), patientDataProtectionService);
        return ResponseEntity.ok(ApiResponse.success("User account updated successfully.", user));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            Authentication authentication,
            @PathVariable String userId) {
        userService.deactivateUser(userId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("User account deactivated successfully.", null));
    }
}
