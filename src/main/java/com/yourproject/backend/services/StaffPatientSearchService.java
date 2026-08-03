package com.yourproject.backend.services;

import java.util.List;

import com.yourproject.backend.dtos.responses.StaffPatientSearchResponse;

public interface StaffPatientSearchService {
    List<StaffPatientSearchResponse> searchByName(String name, int limit);
}
