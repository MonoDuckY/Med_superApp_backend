package com.yourproject.backend.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void oversizedMultipartReturnsPayloadTooLarge() {
        var response = handler.handleMaxUploadSizeExceededException(
                new MaxUploadSizeExceededException(5_242_880L));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals("FILE_TOO_LARGE", response.getBody().getErrorCode());
        assertEquals("Uploaded image must not exceed 5 MB.", response.getBody().getMessage());
    }
}
