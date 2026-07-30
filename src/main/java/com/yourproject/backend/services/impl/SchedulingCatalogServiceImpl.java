package com.yourproject.backend.services.impl;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.yourproject.backend.dtos.requests.CreateClinicRoomRequest;
import com.yourproject.backend.exceptions.ConflictException;
import com.yourproject.backend.exceptions.ForbiddenException;
import com.yourproject.backend.models.ClinicRoom;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.models.WorkSlot;
import com.yourproject.backend.repositories.ClinicRoomRepository;
import com.yourproject.backend.repositories.WorkSlotRepository;
import com.yourproject.backend.services.SchedulingCatalogService;
import com.yourproject.backend.services.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SchedulingCatalogServiceImpl implements SchedulingCatalogService {
    private final WorkSlotRepository workSlotRepository;
    private final ClinicRoomRepository clinicRoomRepository;
    private final UserService userService;

    @Override
    public List<WorkSlot> getActiveWorkSlots() {
        return workSlotRepository.findAllByActiveTrueOrderByStartTimeAsc();
    }

    @Override
    public List<ClinicRoom> getActiveClinicRooms() {
        return clinicRoomRepository.findAllByActiveTrueOrderByCodeAsc();
    }

    @Override
    public ClinicRoom createClinicRoom(String requestedBy, CreateClinicRoomRequest request) {
        User staff = userService.getActiveUserById(requestedBy);
        if (staff.getRole() != UserRole.STAFF && staff.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Only staff or administrators can create clinic rooms.");
        }

        String code = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (clinicRoomRepository.existsByCode(code)) {
            throw new ConflictException("Clinic room code already exists.");
        }

        Instant now = Instant.now();
        return clinicRoomRepository.save(ClinicRoom.builder()
                .code(code)
                .name(request.getName().trim())
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
