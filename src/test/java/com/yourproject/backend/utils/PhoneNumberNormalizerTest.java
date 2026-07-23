package com.yourproject.backend.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.yourproject.backend.exceptions.BadRequestException;

class PhoneNumberNormalizerTest {
    @Test
    void normalize_convertsDomesticVietnamesePhoneNumberToInternationalFormat() {
        assertEquals("+84363636363", PhoneNumberNormalizer.normalize("0363636363"));
    }

    @Test
    void normalize_keepsInternationalVietnamesePhoneNumber() {
        assertEquals("+84363636363", PhoneNumberNormalizer.normalize("+84 363 636 363"));
    }

    @Test
    void normalize_rejectsPhoneNumberWithWrongLength() {
        assertThrows(BadRequestException.class, () -> PhoneNumberNormalizer.normalize("036363636"));
    }

    @Test
    void normalize_rejectsUnsupportedCountryPrefix() {
        assertThrows(BadRequestException.class, () -> PhoneNumberNormalizer.normalize("+85363636363"));
    }
}
