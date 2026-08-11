package com.yourproject.backend.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.yourproject.backend.dtos.requests.CreateUserRequest;
import com.yourproject.backend.dtos.requests.UpdateUserRequest;
import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminUserManagementService {
    private final UserService userService;
    private final UserRepository userRepository;
    private final DoctorCertificateService doctorCertificateService;

    public User create(CreateUserRequest request, MultipartFile certificate, String adminId) {
        validateCertificate(request.getRole(), false, certificate);
        User created = userService.createUser(request, adminId);
        if (request.getRole() != UserRole.DOCTOR) {
            return created;
        }
        try {
            return doctorCertificateService.upload(created, certificate);
        } catch (RuntimeException exception) {
            userRepository.deleteById(created.getId());
            throw exception;
        }
    }

    public User update(String userId, UpdateUserRequest request, MultipartFile certificate, String adminId) {
        User current = userService.getUserById(userId);
        UserRole effectiveRole = request.getRole() == null ? current.getRole() : request.getRole();
        boolean hasStoredCertificate = current.getCertificateObjectKey() != null
                && !current.getCertificateObjectKey().isBlank();
        validateCertificate(effectiveRole, hasStoredCertificate, certificate);

        User updated = userService.updateUser(userId, request, adminId);
        if (effectiveRole == UserRole.DOCTOR && hasFile(certificate)) {
            return doctorCertificateService.upload(updated, certificate);
        }
        if (effectiveRole != UserRole.DOCTOR && updated.getCertificateObjectKey() != null) {
            return doctorCertificateService.remove(updated);
        }
        return updated;
    }

    private void validateCertificate(UserRole role, boolean hasStoredCertificate, MultipartFile certificate) {
        boolean supplied = hasFile(certificate);
        if (role == UserRole.DOCTOR && !hasStoredCertificate && !supplied) {
            throw new BadRequestException("Doctor accounts require a certificate image.");
        }
        if (role != UserRole.DOCTOR && supplied) {
            throw new BadRequestException("Only Doctor accounts can have a certificate image.");
        }
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }
}
