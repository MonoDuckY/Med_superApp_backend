package com.yourproject.backend.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.yourproject.backend.exceptions.BadRequestException;

class BloodPressureUtilsTest {
    @Test
    void normalizePadsEachValueToThreeDigits() {
        assertEquals("120/080", BloodPressureUtils.normalize("120/80"));
        assertEquals("012/008", BloodPressureUtils.normalize("12/8"));
        assertEquals("120/080", BloodPressureUtils.normalize("120/080"));
    }

    @Test
    void normalizeRejectsInvalidFormat() {
        assertThrows(BadRequestException.class, () -> BloodPressureUtils.normalize("120-80"));
        assertThrows(BadRequestException.class, () -> BloodPressureUtils.normalize("120/1000"));
        assertThrows(BadRequestException.class, () -> BloodPressureUtils.normalize("abc/80"));
    }
}
