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
import com.yourproject.backend.exceptions.ForbiddenException;
import com.yourproject.backend.exceptions.ResourceNotFoundException;
import com.yourproject.backend.exceptions.UnauthorizedException;
import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.services.UserService;
import com.yourproject.backend.services.PatientDataProtectionService;
import com.yourproject.backend.utils.PasswordPolicy;
import com.yourproject.backend.utils.PhoneNumberNormalizer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PatientDataProtectionService patientDataProtectionService;

    @Override
    public User createUser(CreateUserRequest request, String createdBy) {
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            throw new BadRequestException("At least one role is required.");
        }
        String phoneNumber = PhoneNumberNormalizer.normalize(request.getPhoneNumber());
        String phoneLookup = patientDataProtectionService.phoneLookup(phoneNumber);
        if (userRepository.existsByPhoneLookup(phoneLookup)) {
            throw new ConflictException("Phone number already exists.");
        }

        boolean patientOnly = request.getRoles().size() == 1 && request.getRoles().contains(UserRole.PATIENT);
        if (!patientOnly) {
            PasswordPolicy.validate(request.getPassword());
        }
        Instant now = Instant.now();
        User user = User.builder()
                .passwordHash(patientOnly ? null : passwordEncoder.encode(request.getPassword()))
                .roles(new java.util.LinkedHashSet<>(request.getRoles()))
                .status(AccountStatus.ACTIVE)
                .patientId(null)
                .fullName(trimToNull(request.getFullName()))
                .gender(trimToNull(request.getGender()))
                .dateOfBirth(request.getDateOfBirth())
                .phoneNumber(phoneNumber)
                .phoneLookup(phoneLookup)
                .address(trimToNull(request.getAddress()))
                .citizenIdentificationCode(trimToNull(request.getCitizenIdentificationCode()))
                .healthInsuranceCode(trimToNull(request.getHealthInsuranceCode()))
                .certificate(trimToNull(request.getCertificate()))
                .createdAt(now)
                .updatedAt(now)
                .passwordChangedAt(now)
                .createdBy(createdBy)
                .build();

        validateAccountProfile(user);
        if (user.getRoles().contains(UserRole.PATIENT)) {
            patientDataProtectionService.encryptPatientFields(user);
        }
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
            throw new UnauthorizedException("This account is inactive. Please contact an administrator.");
        }
        return user;
    }

    @Override
    public User findByPhoneNumber(String phoneNumber) {
        String normalizedPhoneNumber = PhoneNumberNormalizer.normalize(phoneNumber);
        return userRepository.findByPhoneLookup(patientDataProtectionService.phoneLookup(normalizedPhoneNumber))
                .orElseThrow(() -> new UnauthorizedException("Invalid phone number or password."));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    public User updateUser(String userId, UpdateUserRequest request, String updatedBy) {
        User user = getUserById(userId);
        patientDataProtectionService.decryptPatientFields(user);

        java.util.Set<UserRole> effectiveRoles = request.getRoles() == null || request.getRoles().isEmpty()
                ? user.getRoles()
                : request.getRoles();
        if (request.getPassword() != null) {
            if (request.getPassword().isBlank()) {
                throw new BadRequestException("Password must not be blank when provided.");
            }
            if (effectiveRoles.size() == 1 && effectiveRoles.contains(UserRole.PATIENT)) {
                throw new BadRequestException("Patient-only accounts authenticate using SMS OTP and do not use passwords.");
            }
            PasswordPolicy.validate(request.getPassword());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setPasswordChangedAt(Instant.now());
            user.setAccessTokenHash(null);
            user.setRefreshTokenHash(null);
            user.setRefreshTokenExpiresAt(null);
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            user.setRoles(new java.util.LinkedHashSet<>(request.getRoles()));
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (request.getFullName() != null) {
            user.setFullName(trimToNull(request.getFullName()));
        }
        if (request.getGender() != null) {
            user.setGender(trimToNull(request.getGender()));
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getPhoneNumber() != null) {
            String phoneNumber = PhoneNumberNormalizer.normalize(request.getPhoneNumber());
            String phoneLookup = patientDataProtectionService.phoneLookup(phoneNumber);
            if (!phoneLookup.equals(user.getPhoneLookup()) && userRepository.existsByPhoneLookup(phoneLookup)) {
                throw new ConflictException("Phone number already exists.");
            }
            user.setPhoneNumber(phoneNumber);
            user.setPhoneLookup(phoneLookup);
        }
        if (request.getAddress() != null) {
            user.setAddress(trimToNull(request.getAddress()));
        }
        if (request.getCitizenIdentificationCode() != null) {
            user.setCitizenIdentificationCode(trimToNull(request.getCitizenIdentificationCode()));
        }
        if (request.getHealthInsuranceCode() != null) {
            user.setHealthInsuranceCode(trimToNull(request.getHealthInsuranceCode()));
        }
        if (request.getCertificate() != null) {
            user.setCertificate(trimToNull(request.getCertificate()));
        }

        user.setUpdatedAt(Instant.now());
        validateAccountProfile(user);
        if (user.getRoles().contains(UserRole.PATIENT)) {
            patientDataProtectionService.encryptPatientFields(user);
        }
        return userRepository.save(user);
    }

    @Override
    public User toggleUserStatus(String userId, String requestedBy) {
        if (userId.equals(requestedBy)) {
            throw new BadRequestException("You cannot change your own account status.");
        }

        User user = getUserById(userId);
        user.setStatus(user.getStatus() == AccountStatus.ACTIVE ? AccountStatus.INACTIVE : AccountStatus.ACTIVE);
        if (user.getStatus() == AccountStatus.INACTIVE) {
            user.setAccessTokenHash(null);
            user.setRefreshTokenHash(null);
        }
        user.setUpdatedAt(Instant.now());
        return userRepository.save(user);
    }

    @Override
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = getActiveUserById(userId);
        if (user.getRoles().size() == 1 && user.getRoles().contains(UserRole.PATIENT)) {
            throw new ForbiddenException("Patient accounts authenticate using SMS OTP and do not have passwords.");
        }
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
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            throw new BadRequestException("At least one role is required.");
        }

        if (isBlank(user.getPhoneNumber())) {
            throw new BadRequestException("Phone number is required.");
        }
        if (isBlank(user.getFullName())) {
            throw new BadRequestException("Full name is required.");
        }
        user.setPhoneNumber(PhoneNumberNormalizer.normalize(user.getPhoneNumber()));

        if (user.getDateOfBirth() != null && user.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new BadRequestException("Date of birth cannot be in the future.");
        }

        if (user.getRoles().contains(UserRole.DOCTOR) && isBlank(user.getCertificate())) {
            throw new BadRequestException("Doctor accounts require a practice certificate.");
        }
        if (!user.getRoles().contains(UserRole.PATIENT)) {
            return;
        }

        if (isBlank(user.getFullName()) || isBlank(user.getGender()) || user.getDateOfBirth() == null
                || isBlank(user.getPhoneNumber())) {
            throw new BadRequestException(
                    "Patient accounts require full name, gender, date of birth, and phone number.");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
