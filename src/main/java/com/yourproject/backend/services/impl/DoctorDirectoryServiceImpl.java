package com.yourproject.backend.services.impl;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.yourproject.backend.dtos.responses.PatientDoctorResponse;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.services.DoctorDirectoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DoctorDirectoryServiceImpl implements DoctorDirectoryService {
    private final UserRepository userRepository;

    @Override
    public List<PatientDoctorResponse> getActiveDoctors() {
        return userRepository.findActiveDoctors().stream()
                .map(PatientDoctorResponse::from)
                .sorted(Comparator.comparing(
                        PatientDoctorResponse::getFullName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }
}
