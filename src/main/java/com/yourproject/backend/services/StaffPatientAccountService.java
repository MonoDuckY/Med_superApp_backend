package com.yourproject.backend.services;

import org.springframework.stereotype.Service;

import com.yourproject.backend.dtos.requests.CreateUserRequest;
import com.yourproject.backend.dtos.requests.StaffCreatePatientRequest;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StaffPatientAccountService {
    private final UserService userService;

    public User createPatient(StaffCreatePatientRequest request, String staffId) {
        CreateUserRequest createRequest = new CreateUserRequest();
        createRequest.setRole(UserRole.PATIENT);
        createRequest.setFullName(request.getFullName());
        createRequest.setGender(request.getGender());
        createRequest.setDateOfBirth(request.getDateOfBirth());
        createRequest.setPhoneNumber(request.getPhoneNumber());
        createRequest.setAddress(request.getAddress());
        createRequest.setCitizenIdentificationCode(request.getCitizenIdentificationCode());
        createRequest.setHealthInsuranceCode(request.getHealthInsuranceCode());
        createRequest.setMedicalHistory(request.getMedicalHistory());
        createRequest.setCurrentSickness(request.getCurrentSickness());
        createRequest.setHeight(request.getHeight());
        createRequest.setWeight(request.getWeight());
        createRequest.setBloodType(request.getBloodType());
        return userService.createUser(createRequest, staffId);
    }
}
