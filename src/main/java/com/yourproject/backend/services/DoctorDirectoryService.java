package com.yourproject.backend.services;

import java.util.List;

import com.yourproject.backend.dtos.responses.PatientDoctorResponse;

public interface DoctorDirectoryService {
    List<PatientDoctorResponse> getActiveDoctors();
}
