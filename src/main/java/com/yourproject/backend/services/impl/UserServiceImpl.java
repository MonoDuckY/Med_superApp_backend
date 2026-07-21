package com.yourproject.backend.services.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.yourproject.backend.dtos.requests.ChangePasswordRequest;
import com.yourproject.backend.dtos.requests.CreateUserRequest;
import com.yourproject.backend.dtos.requests.UpdateUserRequest;
import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.exceptions.ConflictException;
import com.yourproject.backend.exceptions.ResourceNotFoundException;
import com.yourproject.backend.exceptions.UnauthorizedException;
import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.services.UserService;
import com.yourproject.backend.utils.PasswordPolicy;
import com.yourproject.backend.utils.UsernameNormalizer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User createUser(CreateUserRequest request, String createdBy) {
        String username = UsernameNormalizer.normalizeUsername(request.getUsername());
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already exists.");
        }

        PasswordPolicy.validate(request.getPassword());
        Instant now = Instant.now();
        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(AccountStatus.ACTIVE)
                .patientId(trimToNull(request.getPatientId()))
                .fullName(trimToNull(request.getFullName()))
                .gender(trimToNull(request.getGender()))
                .dateOfBirth(request.getDateOfBirth())
                .phoneNumber(trimToNull(request.getPhoneNumber()))
                .createdAt(now)
                .updatedAt(now)
                .passwordChangedAt(now)
                .createdBy(createdBy)
                .build();

        validateAccountProfile(user);
        return userRepository.save(user);
    }

    @Override
    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found."));
    }

    @Override
    public User getActiveUserById(String userId) {
        User user = getUserById(userId);
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new UnauthorizedException("This account is disabled. Please contact an administrator.");
        }
        return user;
    }

    @Override
    public User findByUsername(String username) {
        String normalizedUsername = UsernameNormalizer.normalizeUsername(username);
        return userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password."));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    public User updateUser(String userId, UpdateUserRequest request, String updatedBy) {
        User user = getUserById(userId);

        if (request.getUsername() != null) {
            String username = UsernameNormalizer.normalizeUsername(request.getUsername());
            if (!username.equals(user.getUsername()) && userRepository.existsByUsername(username)) {
                throw new ConflictException("Username already exists.");
            }
            user.setUsername(username);
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (request.getFullName() != null) {
            user.setFullName(trimToNull(request.getFullName()));
        }
        if (request.getPatientId() != null) {
            user.setPatientId(trimToNull(request.getPatientId()));
        }
        if (request.getGender() != null) {
            user.setGender(trimToNull(request.getGender()));
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        }

        user.setUpdatedAt(Instant.now());
        validateAccountProfile(user);
        return userRepository.save(user);
    }

    @Override
    public void deactivateUser(String userId, String requestedBy) {
        if (userId.equals(requestedBy)) {
            throw new BadRequestException("You cannot deactivate your own account.");
        }

        User user = getUserById(userId);
        user.setStatus(AccountStatus.DISABLED);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    @Override
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = getActiveUserById(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect.");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirmation do not match.");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from the current password.");
        }

        PasswordPolicy.validate(request.getNewPassword());
        Instant now = Instant.now();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);
    }

    @Override
    public void recordSuccessfulLogin(User user) {
        user.setLastLoginAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    private void validateAccountProfile(User user) {
        if (user.getRole() == null) {
            throw new BadRequestException("Role is required.");
        }

        if (user.getRole() != UserRole.PATIENT) {
            validateStaffUsername(user.getUsername());
            user.setPatientId(null);
            user.setGender(null);
            user.setDateOfBirth(null);
            user.setPhoneNumber(null);
            return;
        }

        if (isBlank(user.getPatientId()) || isBlank(user.getFullName()) || isBlank(user.getGender())
                || user.getDateOfBirth() == null || isBlank(user.getPhoneNumber())) {
            throw new BadRequestException(
                    "Patient accounts require patient ID, full name, gender, date of birth, and phone number.");
        }
        if (user.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new BadRequestException("Date of birth cannot be in the future.");
        }

        String phoneNumber = UsernameNormalizer.normalizePhoneNumber(user.getPhoneNumber());
        if (!user.getUsername().equals(phoneNumber)) {
            throw new BadRequestException("A patient username must match the registered phone number.");
        }
        user.setPhoneNumber(phoneNumber);
    }

    private void validateStaffUsername(String username) {
        if (username.length() < 5 || username.length() > 50) {
            throw new BadRequestException("Username must contain 5-50 characters.");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
