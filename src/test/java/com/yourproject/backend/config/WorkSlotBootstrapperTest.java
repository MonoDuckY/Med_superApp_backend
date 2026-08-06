package com.yourproject.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yourproject.backend.models.WorkSession;
import com.yourproject.backend.models.WorkSlot;
import com.yourproject.backend.repositories.WorkSlotRepository;

@ExtendWith(MockitoExtension.class)
class WorkSlotBootstrapperTest {
    @Mock
    private WorkSlotRepository workSlotRepository;

    @Test
    void createsFortySixUniqueThirtyMinuteSlotsIncludingNightSession() throws Exception {
        when(workSlotRepository.findByName(anyString())).thenReturn(Optional.empty());
        WorkSlotBootstrapper bootstrapper = new WorkSlotBootstrapper(workSlotRepository);

        bootstrapper.run(null);

        ArgumentCaptor<WorkSlot> captor = ArgumentCaptor.forClass(WorkSlot.class);
        verify(workSlotRepository, times(46)).save(captor.capture());
        List<WorkSlot> slots = captor.getAllValues();
        assertEquals(46, slots.size());
        assertEquals(46, new HashSet<>(slots.stream().map(WorkSlot::getName).toList()).size());
        assertEquals("Slot1", slots.get(0).getName());
        assertEquals(LocalTime.of(8, 0), slots.get(0).getStartTime());
        assertEquals(LocalTime.of(12, 0), slots.get(7).getEndTime());
        assertEquals(WorkSession.MORNING, slots.get(7).getSession());
        assertEquals("Slot9", slots.get(8).getName());
        assertEquals(LocalTime.of(13, 0), slots.get(8).getStartTime());
        assertEquals(LocalTime.of(17, 0), slots.get(15).getEndTime());
        assertEquals(WorkSession.AFTERNOON, slots.get(15).getSession());
        assertEquals("Slot17", slots.get(16).getName());
        assertEquals(LocalTime.of(17, 0), slots.get(16).getStartTime());
        assertEquals("Slot46", slots.get(45).getName());
        assertEquals(LocalTime.of(7, 30), slots.get(45).getStartTime());
        assertEquals(LocalTime.of(8, 0), slots.get(45).getEndTime());
        assertTrue(slots.subList(16, 46).stream()
                .allMatch(slot -> slot.getSession() == WorkSession.NIGHT));
        assertTrue(slots.stream().allMatch(WorkSlot::isActive));
    }
}
