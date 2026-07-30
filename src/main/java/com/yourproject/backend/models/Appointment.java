package com.yourproject.backend.models;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "appointments")
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

    @Transient
    private String doctorId;

    @Indexed
    private String doctorWorkSlotId;

    @Transient
    private String slotId;
    @Transient
    private String slotName;
    @Transient
    private String roomId;
    @Transient
    private String roomCode;
    @Transient
    private LocalDate appointmentDate;
    @Transient
    private Instant startAt;
    @Transient
    private Instant endAt;

    @Indexed
    private AppointmentStatus status;

    private String diagnosis;
    @Transient
    private String note;
    private Instant requestedAt;
    @Transient
    private String reviewedBy;
    @Transient
    private Instant reviewedAt;
    @Transient
    private String rejectionReason;
    private String cancelledBy;
    private Instant cancelledAt;
    private String cancellationReason;
    @Transient
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;
}
