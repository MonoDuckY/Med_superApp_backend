package com.yourproject.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.yourproject.backend.dtos.requests.CreateUserRequest;
import com.yourproject.backend.dtos.requests.UpdateUserRequest;
import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminUserManagementServiceTest {
    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DoctorCertificateService doctorCertificateService;
    @InjectMocks
    private AdminUserManagementService service;

    @Test
    void createDoctor_requiresCertificateImage() {
        CreateUserRequest request = createRequest(UserRole.DOCTOR);

        assertThrows(BadRequestException.class, () -> service.create(request, null, "admin-id"));
        verify(userService, never()).createUser(request, "admin-id");
    }

    @Test
    void createDoctor_uploadsCertificateAfterCreatingAccount() {
        CreateUserRequest request = createRequest(UserRole.DOCTOR);
        MockMultipartFile certificate = image();
        User doctor = User.builder().id("doctor-id").roleId(UserRole.DOCTOR.getId()).build();
        when(userService.createUser(request, "admin-id")).thenReturn(doctor);
        when(doctorCertificateService.upload(doctor, certificate)).thenReturn(doctor);

        assertEquals(doctor, service.create(request, certificate, "admin-id"));
        verify(doctorCertificateService).upload(doctor, certificate);
    }

    @Test
    void createNonDoctor_rejectsCertificateImage() {
        CreateUserRequest request = createRequest(UserRole.STAFF);

        assertThrows(BadRequestException.class, () -> service.create(request, image(), "admin-id"));
        verify(userService, never()).createUser(request, "admin-id");
    }

    @Test
    void updateDoctor_canKeepExistingCertificateWithoutUploadingNewImage() {
        User doctor = User.builder().id("doctor-id").roleId(UserRole.DOCTOR.getId())
                .certificateObjectKey("doctor-certificates/doctor-id/old.jpg").build();
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Doctor Updated");
        when(userService.getUserById("doctor-id")).thenReturn(doctor);
        when(userService.updateUser("doctor-id", request, "admin-id")).thenReturn(doctor);

        assertEquals(doctor, service.update("doctor-id", request, null, "admin-id"));
        verify(doctorCertificateService, never()).upload(doctor, null);
    }

    @Test
    void updateToDoctor_requiresCertificateWhenNoneIsStored() {
        User staff = User.builder().id("user-id").roleId(UserRole.STAFF.getId()).build();
        UpdateUserRequest request = new UpdateUserRequest();
        request.setRole(UserRole.DOCTOR);
        when(userService.getUserById("user-id")).thenReturn(staff);

        assertThrows(BadRequestException.class,
                () -> service.update("user-id", request, null, "admin-id"));
        verify(userService, never()).updateUser("user-id", request, "admin-id");
    }

    @Test
    void updateNonDoctor_rejectsCertificateImage() {
        User staff = User.builder().id("staff-id").roleId(UserRole.STAFF.getId()).build();
        UpdateUserRequest request = new UpdateUserRequest();
        when(userService.getUserById("staff-id")).thenReturn(staff);

        assertThrows(BadRequestException.class,
                () -> service.update("staff-id", request, image(), "admin-id"));
    }

    @Test
    void changingDoctorToStaffRemovesStoredCertificate() {
        User doctor = User.builder().id("doctor-id").roleId(UserRole.DOCTOR.getId())
                .certificateObjectKey("doctor-certificates/doctor-id/old.jpg").build();
        User updated = User.builder().id("doctor-id").roleId(UserRole.STAFF.getId())
                .certificateObjectKey("doctor-certificates/doctor-id/old.jpg").build();
        UpdateUserRequest request = new UpdateUserRequest();
        request.setRole(UserRole.STAFF);
        when(userService.getUserById("doctor-id")).thenReturn(doctor);
        when(userService.updateUser("doctor-id", request, "admin-id")).thenReturn(updated);
        when(doctorCertificateService.remove(updated)).thenReturn(updated);

        service.update("doctor-id", request, null, "admin-id");

        verify(doctorCertificateService).remove(updated);
    }

    private CreateUserRequest createRequest(UserRole role) {
        CreateUserRequest request = new CreateUserRequest();
        request.setRole(role);
        return request;
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("certificate", "certificate.jpg", "image/jpeg", new byte[] { 1 });
    }
}
