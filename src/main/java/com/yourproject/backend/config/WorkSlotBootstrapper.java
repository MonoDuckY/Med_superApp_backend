package com.yourproject.backend.config;

import java.time.LocalTime;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.yourproject.backend.models.WorkSession;
import com.yourproject.backend.models.WorkSlot;
import com.yourproject.backend.repositories.WorkSlotRepository;

import lombok.RequiredArgsConstructor;

@Component
@Order(1)
@RequiredArgsConstructor
public class WorkSlotBootstrapper implements ApplicationRunner {
    private final WorkSlotRepository workSlotRepository;

    @Override
    public void run(ApplicationArguments args) {
        for (int index = 0; index < 46; index++) {
            String name = "Slot" + (index + 1);
            WorkSession session;
            LocalTime startTime;
            if (index < 8) {
                session = WorkSession.MORNING;
                startTime = LocalTime.of(8, 0).plusMinutes(index * 30L);
            } else if (index < 16) {
                session = WorkSession.AFTERNOON;
                startTime = LocalTime.of(13, 0).plusMinutes((index - 8) * 30L);
            } else {
                session = WorkSession.NIGHT;
                startTime = LocalTime.of(17, 0).plusMinutes((index - 16) * 30L);
            }
            LocalTime endTime = startTime.plusMinutes(30);

            WorkSlot slot = workSlotRepository.findByName(name).orElseGet(WorkSlot::new);
            slot.setName(name);
            slot.setStartTime(startTime);
            slot.setEndTime(endTime);
            slot.setSession(session);
            slot.setActive(true);
            workSlotRepository.save(slot);
        }
    }
}
