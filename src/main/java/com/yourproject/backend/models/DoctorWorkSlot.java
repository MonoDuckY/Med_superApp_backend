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

@Document(collection = "doctor_work_slots")
@CompoundIndexes({
        @CompoundIndex(
                name = "active_room_work_slot_unique",
                def = "{'workDate': 1, 'slotId': 1, 'roomId': 1}",
                unique = true,
                partialFilter = "{'conflictActive': true}"),
        @CompoundIndex(
                name = "active_doctor_work_slot_unique",
                def = "{'workDate': 1, 'slotId': 1, 'doctorId': 1}",
                unique = true,
                partialFilter = "{'conflictActive': true}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorWorkSlot {
    @Id
    private String id;

    @Indexed
    private String submissionId;

    @Indexed
    private String doctorId;

    @Indexed
    private LocalDate workDate;

    private String slotId;
    private String slotName;
    private String roomId;
    private String roomCode;
    private Instant startAt;
    private Instant endAt;

    @Indexed
    private WorkSlotApprovalStatus approvalStatus;

    @Indexed
    private WorkSlotBookingStatus bookingStatus;

    private String note;
    private Instant submittedAt;
    private String reviewedBy;
    private Instant reviewedAt;
    private String rejectionReason;
    private boolean conflictActive;
    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;
}
