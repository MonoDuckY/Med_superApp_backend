package com.yourproject.backend.services;

import java.util.List;

import com.yourproject.backend.dtos.requests.CreateClinicRoomRequest;
import com.yourproject.backend.models.ClinicRoom;
import com.yourproject.backend.models.WorkSlot;

public interface SchedulingCatalogService {
    List<WorkSlot> getActiveWorkSlots();

    List<ClinicRoom> getActiveClinicRooms();

    ClinicRoom createClinicRoom(String requestedBy, CreateClinicRoomRequest request);
}
