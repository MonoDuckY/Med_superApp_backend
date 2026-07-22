package com.yourproject.backend.utils;

import com.yourproject.backend.exceptions.BadRequestException;

public final class PhoneNumberNormalizer {
    private PhoneNumberNormalizer() {
    }

    public static String normalize(String rawPhoneNumber) {
        if (rawPhoneNumber == null || rawPhoneNumber.isBlank()) {
            throw new BadRequestException("Phone number is required.");
        }

        String phoneNumber = rawPhoneNumber.replaceAll("[\\s()\\-]", "");
        if (phoneNumber.matches("0\\d{9}")) {
            phoneNumber = "+84" + phoneNumber.substring(1);
        }

        if (!phoneNumber.matches("\\+84\\d{9}")) {
            throw new BadRequestException(
                    "Phone number must be a valid Vietnamese number: 10 digits starting with 0 or international format +84.");
        }

        return phoneNumber;
    }
}
