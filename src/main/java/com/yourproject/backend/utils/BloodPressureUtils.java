package com.yourproject.backend.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.yourproject.backend.exceptions.BadRequestException;

public final class BloodPressureUtils {
    private static final Pattern BLOOD_PRESSURE_PATTERN = Pattern.compile("^(\\d{1,3})/(\\d{1,3})$");

    private BloodPressureUtils() {
    }

    public static String normalize(String bloodPressure) {
        if (bloodPressure == null) return null;
        Matcher matcher = BLOOD_PRESSURE_PATTERN.matcher(bloodPressure);
        if (!matcher.matches()) {
            throw new BadRequestException(
                    "Blood pressure must use the format xxx/xxx with digits only, for example 120/80.");
        }
        return "%03d/%03d".formatted(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)));
    }
}
