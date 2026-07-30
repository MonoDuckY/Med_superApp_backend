package com.yourproject.backend.models;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "appointments")
@CompoundIndexes({
        @CompoundIndex(
                name = "active_work_slot_appointment_unique",
                def = "{'doctorWorkSlotId': 1}",
                unique = true,
                partialFilter = "{'active': true}"),
        @CompoundIndex(
                name = "active_patient_appointment_day_unique",
                def = "{'patientUserId': 1, 'appointmentDate': 1}",
                unique = true,
                partialFilter = "{'active': true}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {
    @Id
    private String id;

    @Indexed
    private String patientUserId;

    private String patientId;

    @Indexed
    private String doctorId;

    @Indexed
    private String doctorWorkSlotId;

    private String slotId;
    private String slotName;
    private String roomId;
    private String roomCode;
    private LocalDate appointmentDate;
    private Instant startAt;
    private Instant endAt;

    @Indexed
    private AppointmentStatus status;

    private String note;
    private Instant requestedAt;
    private String reviewedBy;
    private Instant reviewedAt;
    private String rejectionReason;
    private String cancelledBy;
    private Instant cancelledAt;
    private String cancellationReason;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;
}
