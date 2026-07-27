package com.yourproject.backend.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.yourproject.backend.dtos.requests.CreateUserRequest;
import com.yourproject.backend.dtos.requests.ChangePasswordRequest;
import com.yourproject.backend.dtos.requests.UpdateUserRequest;
import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.exceptions.ConflictException;
import com.yourproject.backend.exceptions.ResourceNotFoundException;
import com.yourproject.backend.exceptions.UnauthorizedException;
import com.yourproject.backend.models.AccountStatus;
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
        request.setDateOfBirth(LocalDate.now().plusDays(1));
        when(patientDataProtectionService.phoneLookup("+84912345678")).thenReturn("phone-lookup");
        when(userRepository.existsByPhoneLookup("phone-lookup")).thenReturn(false);
        when(passwordEncoder.encode("Newabc123!")).thenReturn("bcrypt-hash");

        assertThrows(BadRequestException.class, () -> userService.createUser(request, "admin-id"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_generatesPatientId() {
        CreateUserRequest request = patientRequest();
        request.setPassword(null);
        when(patientDataProtectionService.phoneLookup("+84912345678")).thenReturn("phone-lookup");
        when(userRepository.existsByPhoneLookup("phone-lookup")).thenReturn(false);
        when(patientDataProtectionService.patientIdLookup(any())).thenReturn("patient-lookup");
        when(userRepository.existsByPatientIdLookup("patient-lookup")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User createdUser = userService.createUser(request, "admin-id");

        assertNotNull(createdUser.getPatientId());
        assertNull(createdUser.getPasswordHash());
        assertEquals(true, createdUser.getPatientId().startsWith("PAT-"));
        verify(patientDataProtectionService).encryptPatientFields(createdUser);
    }

    @Test
    void createUser_rejectsRequestWithoutRole() {
        CreateUserRequest request = doctorRequest();
        request.setRole(null);
        stubDoctorPhone();
        assertThrows(BadRequestException.class, () -> userService.createUser(request, "admin-id"));
    }

    @Test
    void createUser_rejectsRequestWithoutFullName() {
        CreateUserRequest request = doctorRequest();
        request.setFullName(null);
        assertThrows(BadRequestException.class, () -> userService.createUser(request, "admin-id"));
    }

    @Test
    void createUser_rejectsRequestWithoutPhoneNumber() {
        CreateUserRequest request = doctorRequest();
        request.setPhoneNumber(null);

        assertThrows(BadRequestException.class, () -> userService.createUser(request, "admin-id"));
    }

    @Test
    void createUser_rejectsPatientWithoutGender() {
        CreateUserRequest request = patientRequest();
        request.setGender(null);
        stubPatientPhone();
        assertThrows(BadRequestException.class, () -> userService.createUser(request, "admin-id"));
    }

    @Test
    void createUser_rejectsPatientWithoutDateOfBirth() {
        CreateUserRequest request = patientRequest();
        request.setDateOfBirth(null);
        stubPatientPhone();
        assertThrows(BadRequestException.class, () -> userService.createUser(request, "admin-id"));
    }

    @Test
    void getUserById_returnsExistingUser() {
        User user = activeDoctor();
        when(userRepository.findById("user-id")).thenReturn(Optional.of(user));

        assertEquals(user, userService.getUserById("user-id"));
    }

    @Test
    void getUserById_rejectsUnknownUser() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById("missing"));
    }

    @Test
    void getActiveUserById_rejectsDisabledUser() {
        User user = activeDoctor();
        user.setStatus(AccountStatus.DISABLED);
        when(userRepository.findById("user-id")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> userService.getActiveUserById("user-id"));
    }

    @Test
    void findByPhoneNumber_rejectsUnknownPhoneLookup() {
        when(patientDataProtectionService.phoneLookup("+84363636363")).thenReturn("phone-lookup");
        when(userRepository.findByPhoneLookup("phone-lookup")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> userService.findByPhoneNumber("0363636363"));
    }

    @Test
    void updateUser_updatesExistingUser() {
        User user = activeDoctor();
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Dr Updated");
        when(userRepository.findById("user-id")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        assertEquals("Dr Updated", userService.updateUser("user-id", request, "admin-id").getFullName());
        verify(userRepository).save(user);
    }

    @Test
    void deactivateUser_rejectsSelfDeactivation() {
        assertThrows(BadRequestException.class, () -> userService.deactivateUser("user-id", "user-id"));
    }

    @Test
    void deactivateUser_disablesOtherUser() {
        User user = activeDoctor();
        when(userRepository.findById("user-id")).thenReturn(Optional.of(user));

        userService.deactivateUser("user-id", "admin-id");

        assertEquals(AccountStatus.DISABLED, user.getStatus());
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_rejectsIncorrectCurrentPassword() {
        User user = activeDoctor();
        when(userRepository.findById("user-id")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Wrongpass1", "password-hash")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> userService.changePassword("user-id", changePasswordRequest("Wrongpass1")));
    }

    @Test
    void recordSuccessfulLogin_updatesLoginTimestamp() {
        User user = activeDoctor();

        userService.recordSuccessfulLogin(user);

        assertNotNull(user.getLastLoginAt());
        verify(userRepository).save(user);
    }

    private CreateUserRequest doctorRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setPassword("Newabc123!");
        request.setRole(UserRole.DOCTOR);
        request.setFullName("Dr Nguyen");
        request.setPhoneNumber("0363636363");
        request.setCertificate("Practice certificate");
        return request;
    }

    private CreateUserRequest patientRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setPassword("Newabc123!");
        request.setRole(UserRole.PATIENT);
        request.setFullName("Nguyen Van A");
        request.setGender("MALE");
        request.setPhoneNumber("0912345678");
        request.setDateOfBirth(LocalDate.of(1995, 1, 1));
        return request;
    }

    private void stubDoctorPhone() {
        when(patientDataProtectionService.phoneLookup("+84363636363")).thenReturn("phone-lookup");
        when(userRepository.existsByPhoneLookup("phone-lookup")).thenReturn(false);
    }

    private void stubPatientPhone() {
        when(patientDataProtectionService.phoneLookup("+84912345678")).thenReturn("phone-lookup");
        when(userRepository.existsByPhoneLookup("phone-lookup")).thenReturn(false);
    }

    private User activeDoctor() {
        return User.builder().id("user-id").role(UserRole.DOCTOR).status(AccountStatus.ACTIVE)
                .fullName("Dr Nguyen").phoneNumber("+84363636363").phoneLookup("phone-lookup")
                .passwordHash("password-hash").certificate("Practice certificate").createdAt(Instant.now()).build();
    }

    private ChangePasswordRequest changePasswordRequest(String currentPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword("Newpass1");
        request.setConfirmPassword("Newpass1");
        return request;
    }
}
