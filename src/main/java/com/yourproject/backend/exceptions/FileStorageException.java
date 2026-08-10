package com.yourproject.backend.exceptions;

import org.springframework.http.HttpStatus;

public class FileStorageException extends ApiException {
    public FileStorageException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "FILE_STORAGE_ERROR", message);
    }
}
