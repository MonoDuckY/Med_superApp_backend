package com.yourproject.backend.dtos.responses;

import com.yourproject.backend.models.ClinicRoom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClinicRoomResponse {
    private String id;
    private String code;
    private String name;
    private boolean active;

    public static ClinicRoomResponse from(ClinicRoom room) {
        return ClinicRoomResponse.builder()
                .id(room.getId())
                .code(room.getCode())
                .name(room.getName())
                .active(room.isActive())
                .build();
    }
}
