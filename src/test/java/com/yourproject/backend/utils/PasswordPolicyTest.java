package com.yourproject.backend.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.yourproject.backend.exceptions.BadRequestException;

class PasswordPolicyTest {
    @Test
    void validate_acceptsPasswordContainingAllRequiredCharacterClasses() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("Validpass1"));
    }

    @Test
    void validate_rejectsPasswordShorterThanEightCharacters() {
        assertThrows(BadRequestException.class, () -> PasswordPolicy.validate("Abc1def"));
    }

    @Test
    void validate_rejectsPasswordWithoutUppercaseCharacter() {
        assertThrows(BadRequestException.class, () -> PasswordPolicy.validate("validpass1"));
    }

    @Test
    void validate_rejectsPasswordWithoutLowercaseCharacter() {
        assertThrows(BadRequestException.class, () -> PasswordPolicy.validate("VALIDPASS1"));
    }

    @Test
    void validate_acceptsSpecialCharacterInsteadOfDigit() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("Validpass!"));
    }
}
