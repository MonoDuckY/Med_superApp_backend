package com.yourproject.backend.utils;

import java.util.Locale;

import com.yourproject.backend.exceptions.BadRequestException;

public final class UsernameNormalizer {
    private UsernameNormalizer() {
    }

    public static String normalizeUsername(String rawUsername) {
        if (rawUsername == null || rawUsername.isBlank()) {
            throw new BadRequestException("Username is required.");
        }

        String username = rawUsername.trim();
        if (username.startsWith("+84") || username.matches("0\\d{9}")) {
            return normalizePhoneNumber(username);
        }

        return username.toLowerCase(Locale.ROOT);
    }

    public static String normalizePhoneNumber(String rawPhoneNumber) {
        if (rawPhoneNumber == null || rawPhoneNumber.isBlank()) {
            throw new BadRequestException("Phone number is required.");
        }

        String phoneNumber = rawPhoneNumber.replaceAll("[\\s-]", "");
        if (phoneNumber.startsWith("+84")) {
            phoneNumber = "0" + phoneNumber.substring(3);
        }

        if (!phoneNumber.matches("0\\d{9}")) {
            throw new BadRequestException("Phone number must be a valid 10-digit Vietnamese number.");
        }

        return phoneNumber;
    }
}
