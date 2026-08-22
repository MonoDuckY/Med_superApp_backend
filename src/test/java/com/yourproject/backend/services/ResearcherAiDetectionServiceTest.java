package com.yourproject.backend.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.exceptions.FileStorageException;

@ExtendWith(MockitoExtension.class)
class ResearcherAiDetectionServiceTest {

    private static final byte[] VALID_PNG_BYTES = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0 };

    @InjectMocks
    private ResearcherAiDetectionService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "aiBackendUrl", "http://127.0.0.1:8000");
    }

    // TC-UNIT-ResearcherAiDetectionService-001
    @Test
    void detect_Success() {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", MediaType.IMAGE_PNG_VALUE, VALID_PNG_BYTES);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode mockNode = mapper.createObjectNode();
        mockNode.put("result", "success");

        try (MockedStatic<RestClient> mockedRestClient = mockStatic(RestClient.class)) {
            RestClient restClient = mock(RestClient.class);
            RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            mockedRestClient.when(RestClient::create).thenReturn(restClient);
            when(restClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
            when(requestBodySpec.body(any(MultiValueMap.class))).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(JsonNode.class)).thenReturn(mockNode);

            JsonNode result = service.detect(file);
            assertNotNull(result);
            assertEquals("success", result.get("result").asText());
        }
    }

    // TC-UNIT-ResearcherAiDetectionService-002
    @Test
    void detect_ThrowsBadRequestException_WhenImageIsNull() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.detect(null));
        assertEquals("Input image is required.", exception.getMessage());
    }

    // TC-UNIT-ResearcherAiDetectionService-003
    @Test
    void detect_ThrowsBadRequestException_WhenImageIsEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", MediaType.IMAGE_PNG_VALUE, new byte[0]);
        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.detect(file));
        assertEquals("Input image is required.", exception.getMessage());
    }

    // TC-UNIT-ResearcherAiDetectionService-004
    @Test
    void detect_ThrowsFileStorageException_WhenBackendReturnsEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", MediaType.IMAGE_PNG_VALUE, VALID_PNG_BYTES);

        try (MockedStatic<RestClient> mockedRestClient = mockStatic(RestClient.class)) {
            RestClient restClient = mock(RestClient.class);
            RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            mockedRestClient.when(RestClient::create).thenReturn(restClient);
            when(restClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
            when(requestBodySpec.body(any(MultiValueMap.class))).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(JsonNode.class)).thenReturn(null);

            FileStorageException exception = assertThrows(FileStorageException.class, () -> service.detect(file));
            assertEquals("AI backend returned an empty response.", exception.getMessage());
        }
    }

    // TC-UNIT-ResearcherAiDetectionService-005
    @Test
    void detect_ThrowsFileStorageException_WhenRestClientExceptionThrown() {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", MediaType.IMAGE_PNG_VALUE, VALID_PNG_BYTES);

        try (MockedStatic<RestClient> mockedRestClient = mockStatic(RestClient.class)) {
            RestClient restClient = mock(RestClient.class);
            RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            mockedRestClient.when(RestClient::create).thenReturn(restClient);
            when(restClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
            when(requestBodySpec.body(any(MultiValueMap.class))).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenThrow(new RestClientException("Connection refused"));

            FileStorageException exception = assertThrows(FileStorageException.class, () -> service.detect(file));
            assertEquals("Unable to connect to the AI detection backend.", exception.getMessage());
        }
    }
}
