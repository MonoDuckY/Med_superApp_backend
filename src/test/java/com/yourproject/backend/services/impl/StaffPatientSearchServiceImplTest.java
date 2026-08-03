package com.yourproject.backend.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.services.PatientDataProtectionService;

@ExtendWith(MockitoExtension.class)
class StaffPatientSearchServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientDataProtectionService patientDataProtectionService;

    @InjectMocks
    private StaffPatientSearchServiceImpl service;

    @Test
    void searchByName_normalizesVietnameseNamesAndReturnsClosestLimitedResults() {
        when(userRepository.findActivePatients()).thenReturn(List.of(
                patient("3", "Nguyen Van Binh"),
                patient("1", "Nguyễn Văn An"),
                patient("2", "Nguyễn Văn Anh"),
                patient("4", "Trần Minh Đức")));

        var results = service.searchByName("nguyen van an", 2);

        assertEquals(2, results.size());
        assertEquals("Nguyễn Văn An", results.get(0).getFullName());
        assertEquals("Nguyễn Văn Anh", results.get(1).getFullName());
    }

    @Test
    void searchByName_supportsTypoMatching() {
        when(userRepository.findActivePatients()).thenReturn(List.of(
                patient("1", "Nguyễn Văn An"),
                patient("2", "Trần Minh Đức")));

        var results = service.searchByName("nguyen van am", 1);

        assertEquals("Nguyễn Văn An", results.get(0).getFullName());
    }

    @Test
    void searchByName_rejectsBlankNameAndInvalidLimit() {
        assertThrows(BadRequestException.class, () -> service.searchByName("  ", 5));
        assertThrows(BadRequestException.class, () -> service.searchByName("Nguyen", 0));
        assertThrows(BadRequestException.class, () -> service.searchByName("Nguyen", 51));
    }

    private User patient(String id, String fullName) {
        return User.builder()
                .id(id)
                .roles(java.util.Set.of(UserRole.PATIENT))
                .status(AccountStatus.ACTIVE)
                .fullName(fullName)
                .phoneNumber("+8490000000" + id)
                .build();
    }
}
