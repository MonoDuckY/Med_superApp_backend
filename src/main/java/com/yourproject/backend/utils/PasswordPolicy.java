package com.yourproject.backend.utils;

import com.yourproject.backend.exceptions.BadRequestException;

public final class PasswordPolicy {
    private PasswordPolicy() {
    }

    public static void validate(String password) {
        boolean hasDigit = password != null && password.chars().anyMatch(Character::isDigit);
        boolean hasSpecialCharacter = password != null
                && password.chars().anyMatch(character -> !Character.isLetterOrDigit(character));

        if (password == null
                || password.length() < 8
                || password.length() > 50
                || !password.matches(".*[a-z].*")
                || !password.matches(".*[A-Z].*")
                || !(hasDigit || hasSpecialCharacter)) {
            throw new BadRequestException(
                    "Password must contain 8-50 characters, including lowercase, uppercase, and a number or special character.");
        }
    }
}
