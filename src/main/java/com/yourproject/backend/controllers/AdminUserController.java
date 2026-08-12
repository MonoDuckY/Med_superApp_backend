package com.yourproject.backend.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

import com.yourproject.backend.dtos.requests.CreateUserRequest;
import com.yourproject.backend.dtos.requests.UpdateUserRequest;
import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.dtos.responses.UserResponse;
import com.yourproject.backend.services.UserService;
import com.yourproject.backend.services.DoctorCertificateService;
import com.yourproject.backend.services.AdminUserManagementService;
import com.yourproject.backend.services.S3StorageService.PresignedObjectUrl;
import com.yourproject.backend.services.PatientDataProtectionService;
import com.yourproject.backend.models.UserRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService userService;
    private final PatientDataProtectionService patientDataProtectionService;
    private final DoctorCertificateService doctorCertificateService;
    private final AdminUserManagementService adminUserManagementService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequestBody(content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            encoding = {
                    @Encoding(name = "user", contentType = MediaType.APPLICATION_JSON_VALUE),
                    @Encoding(name = "certificate", contentType = "image/png, image/jpeg, image/webp")
            }))
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            Authentication authentication,
            @Valid @RequestPart("user") CreateUserRequest request,
            @RequestPart(value = "certificate", required = false) MultipartFile certificate) {
        var created = adminUserManagementService.create(request, certificate, authentication.getName());
        UserResponse user = adminResponse(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("User account created successfully.", user));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers(
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String citizenIdentificationCode,
            @RequestParam(required = false) UserRole role) {
        List<UserResponse> users = userService.searchUsers(phoneNumber, citizenIdentificationCode, role).stream()
                .map(this::adminResponse).toList();
        return ResponseEntity.ok(ApiResponse.success("User accounts retrieved successfully.", users));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable String userId) {
        UserResponse user = adminResponse(userService.getUserById(userId));
        return ResponseEntity.ok(ApiResponse.success("User account retrieved successfully.", user));
    }

    @PatchMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequestBody(content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            encoding = {
                    @Encoding(name = "user", contentType = MediaType.APPLICATION_JSON_VALUE),
                    @Encoding(name = "certificate", contentType = "image/png, image/jpeg, image/webp")
            }))
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            Authentication authentication,
            @PathVariable String userId,
            @Valid @RequestPart("user") UpdateUserRequest request,
            @RequestPart(value = "certificate", required = false) MultipartFile certificate) {
        var updated = adminUserManagementService.update(userId, request, certificate, authentication.getName());
        UserResponse user = adminResponse(updated);
        return ResponseEntity.ok(ApiResponse.success("User account updated successfully.", user));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(
            Authentication authentication,
            @PathVariable String userId) {
        UserResponse user = UserResponse.from(userService.toggleUserStatus(userId, authentication.getName()), patientDataProtectionService);
        return ResponseEntity.ok(ApiResponse.success("User account status changed successfully.", user));
    }

    private UserResponse adminResponse(com.yourproject.backend.models.User user) {
        PresignedObjectUrl url = doctorCertificateService.createPresignedUrl(user);
        return UserResponse.from(user, patientDataProtectionService, url == null ? null : url.url());
    }
}
