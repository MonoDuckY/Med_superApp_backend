package com.yourproject.backend.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yourproject.backend.dtos.responses.PatientDoctorResponse;
import com.yourproject.backend.models.User;
import com.yourproject.backend.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class DoctorDirectoryServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DoctorDirectoryServiceImpl service;

    @Test
    void getActiveDoctorsReturnsMinimalDataSortedByFullName() {
        User second = User.builder()
                .id("doctor-2")
                .fullName("Tran Van B")
                .phoneNumber("+84922222222")
                .build();
        User first = User.builder()
                .id("doctor-1")
                .fullName("Nguyen Van A")
                .phoneNumber("+84911111111")
                .build();
        when(userRepository.findActiveDoctors()).thenReturn(List.of(second, first));

        List<PatientDoctorResponse> result = service.getActiveDoctors();

        assertEquals(2, result.size());
        assertEquals("doctor-1", result.get(0).getId());
        assertEquals("Nguyen Van A", result.get(0).getFullName());
        assertEquals("+84911111111", result.get(0).getPhoneNumber());
        assertEquals("doctor-2", result.get(1).getId());
    }
}
