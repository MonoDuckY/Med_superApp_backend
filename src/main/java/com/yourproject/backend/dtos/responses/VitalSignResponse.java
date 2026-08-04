package com.yourproject.backend.dtos.responses;

import com.yourproject.backend.models.VitalSign;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VitalSignResponse {
    String id;
    String vitalName;
    String vitalNumber;
    String vitalUnit;
    String appointmentId;

    public static VitalSignResponse from(VitalSign vitalSign) {
        return VitalSignResponse.builder()
                .id(vitalSign.getId())
                .vitalName(vitalSign.getVitalName())
                .vitalNumber(vitalSign.getVitalNumber())
                .vitalUnit(vitalSign.getVitalUnit())
                .appointmentId(vitalSign.getAppointmentId())
                .build();
    }
}
