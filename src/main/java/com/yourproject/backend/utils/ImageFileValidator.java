package com.yourproject.backend.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.springframework.web.multipart.MultipartFile;

import com.yourproject.backend.exceptions.BadRequestException;

public final class ImageFileValidator {
    private ImageFileValidator() {
    }

    public static void validate(MultipartFile file, String fieldName) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(fieldName + " image is required.");
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!contentType.equals("image/jpeg")
                && !contentType.equals("image/png")
                && !contentType.equals("image/webp")) {
            throw new BadRequestException(fieldName + " must be a JPEG, PNG, or WEBP image.");
        }

        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            if (!hasValidSignature(header, contentType)) {
                throw new BadRequestException(fieldName + " is not a valid image file.");
            }
        } catch (IOException exception) {
            throw new BadRequestException("Unable to read the " + fieldName + " image.");
        }
    }

    private static boolean hasValidSignature(byte[] header, String contentType) {
        if (contentType.equals("image/jpeg")) {
            return header.length >= 3 && (header[0] & 0xff) == 0xff
                    && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff;
        }
        if (contentType.equals("image/png")) {
            return header.length >= 8 && (header[0] & 0xff) == 0x89
                    && header[1] == 0x50 && header[2] == 0x4e && header[3] == 0x47
                    && header[4] == 0x0d && header[5] == 0x0a && header[6] == 0x1a && header[7] == 0x0a;
        }
        return header.length >= 12 && header[0] == 'R' && header[1] == 'I'
                && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
    }
}
