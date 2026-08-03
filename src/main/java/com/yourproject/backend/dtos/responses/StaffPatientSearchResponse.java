package com.yourproject.backend.dtos.responses;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StaffPatientSearchResponse {
    String id;
    String fullName;
}
