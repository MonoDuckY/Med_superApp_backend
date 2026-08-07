package com.yourproject.backend.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.yourproject.backend.models.WorkSlot;

public final class WorkSlotTimeUtils {
    private static final LocalTime NIGHT_START = LocalTime.of(17, 0);
    private static final LocalTime NIGHT_END = LocalTime.of(8, 0);

    private WorkSlotTimeUtils() {
    }

    public static boolean isNight(WorkSlot slot) {
        return isNight(slot.getStartTime());
    }

    public static boolean isNight(LocalTime startTime) {
        return startTime != null
                && (!startTime.isBefore(NIGHT_START) || startTime.isBefore(NIGHT_END));
    }

    public static LocalDateTime resolveStart(LocalDate workDate, LocalTime startTime) {
        LocalDate actualDate = startTime.isBefore(NIGHT_END) ? workDate.plusDays(1) : workDate;
        return actualDate.atTime(startTime);
    }

    public static LocalDateTime resolveEnd(LocalDate workDate, LocalTime startTime, LocalTime endTime) {
        LocalDateTime start = resolveStart(workDate, startTime);
        LocalDateTime end = start.toLocalDate().atTime(endTime);
        return end.isAfter(start) ? end : end.plusDays(1);
    }
}
