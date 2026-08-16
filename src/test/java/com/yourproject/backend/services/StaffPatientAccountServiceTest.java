package com.yourproject.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yourproject.backend.dtos.requests.CreateUserRequest;
import com.yourproject.backend.dtos.requests.StaffCreatePatientRequest;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;

@ExtendWith(MockitoExtension.class)
class StaffPatientAccountServiceTest {
    @Mock
    private UserService userService;

    @InjectMocks
    private StaffPatientAccountService service;

    @Test
    void createsPatientWithImplicitRoleAndNoPassword() {
        StaffCreatePatientRequest request = request();
        User patient = User.builder().id("patient-1").roleId(UserRole.PATIENT.getId()).build();
        when(userService.createUser(any(CreateUserRequest.class), eq("staff-1"))).thenReturn(patient);

        User result = service.createPatient(request, "staff-1");

        assertEquals(patient, result);
        ArgumentCaptor<CreateUserRequest> captor = ArgumentCaptor.forClass(CreateUserRequest.class);
        verify(userService).createUser(captor.capture(), eq("staff-1"));
        assertEquals(UserRole.PATIENT, captor.getValue().getRole());
        assertNull(captor.getValue().getPassword());
        assertEquals("Patient Name", captor.getValue().getFullName());
        assertEquals("+84911111111", captor.getValue().getPhoneNumber());
    }

    private StaffCreatePatientRequest request() {
        StaffCreatePatientRequest request = new StaffCreatePatientRequest();
        request.setFullName("Patient Name");
        request.setGender("NONE");
        request.setDateOfBirth(java.time.LocalDate.of(1995, 1, 1));
        request.setPhoneNumber("+84911111111");
        return request;
    }
}
