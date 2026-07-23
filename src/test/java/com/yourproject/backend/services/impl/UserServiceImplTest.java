package com.yourproject.backend.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.yourproject.backend.dtos.requests.CreateUserRequest;
import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.exceptions.ConflictException;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.services.PatientDataProtectionService;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PatientDataProtectionService patientDataProtectionService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void createUser_createsDoctorWithNormalizedPhoneNumberAndHashedPassword() {
        CreateUserRequest request = doctorRequest();
        when(patientDataProtectionService.phoneLookup("+84363636363")).thenReturn("phone-lookup");
        when(userRepository.existsByPhoneLookup("phone-lookup")).thenReturn(false);
        when(passwordEncoder.encode("Newabc123!")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User createdUser = userService.createUser(request, "admin-id");

        assertEquals("+84363636363", createdUser.getPhoneNumber());
        assertEquals("bcrypt-hash", createdUser.getPasswordHash());
        assertEquals(UserRole.DOCTOR, createdUser.getRole());
        assertNull(createdUser.getPatientId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_rejectsDuplicatePhoneNumber() {
        CreateUserRequest request = doctorRequest();
        when(patientDataProtectionService.phoneLookup("+84363636363")).thenReturn("phone-lookup");
        when(userRepository.existsByPhoneLookup("phone-lookup")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.createUser(request, "admin-id"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_rejectsDoctorWithoutPracticeCertificate() {
        CreateUserRequest request = doctorRequest();
        request.setCertificate(null);
        when(patientDataProtectionService.phoneLookup("+84363636363")).thenReturn("phone-lookup");
        when(userRepository.existsByPhoneLookup("phone-lookup")).thenReturn(false);
        when(passwordEncoder.encode("Newabc123!")).thenReturn("bcrypt-hash");

        assertThrows(BadRequestException.class, () -> userService.createUser(request, "admin-id"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_rejectsPatientWithFutureDateOfBirth() {
        CreateUserRequest request = patientRequest();
        when(patientDataProtectionService.phoneLookup("+84912345678")).thenReturn("phone-lookup");
        when(userRepository.existsByPhoneLookup("phone-lookup")).thenReturn(false);
        when(passwordEncoder.encode("Newabc123!")).thenReturn("bcrypt-hash");

        assertThrows(BadRequestException.class, () -> userService.createUser(request, "admin-id"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_rejectsPatientWithoutPatientId() {
        CreateUserRequest request = patientRequest();
        request.setPatientId(null);
        when(patientDataProtectionService.phoneLookup("+84912345678")).thenReturn("phone-lookup");
        when(userRepository.existsByPhoneLookup("phone-lookup")).thenReturn(false);
        when(passwordEncoder.encode("Newabc123!")).thenReturn("bcrypt-hash");

        assertThrows(BadRequestException.class, () -> userService.createUser(request, "admin-id"));
        verify(userRepository, never()).save(any(User.class));
    }

    private CreateUserRequest doctorRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setPassword("Newabc123!");
        request.setRole(UserRole.DOCTOR);
        request.setFullName("Dr Nguyen");
        request.setPhoneNumber("0363636363");
        request.setCertificate("Practice certificate");
        request.setPatientId("should-be-ignored");
        return request;
    }

    private CreateUserRequest patientRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setPassword("Newabc123!");
        request.setRole(UserRole.PATIENT);
        request.setFullName("Nguyen Van A");
        request.setPatientId("PAT-001");
        request.setGender("MALE");
        request.setPhoneNumber("0912345678");
        request.setDateOfBirth(LocalDate.now().plusDays(1));
        return request;
    }
}
