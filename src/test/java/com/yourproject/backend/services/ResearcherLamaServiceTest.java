package com.yourproject.backend.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.exceptions.FileStorageException;
import com.yourproject.backend.services.ResearcherLamaService.ProcessedImage;

@ExtendWith(MockitoExtension.class)
class ResearcherLamaServiceTest {

    @InjectMocks
    private ResearcherLamaService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "lamaBackendUrl", "http://18.143.151.200:8000");
    }

    // TC-UNIT-ResearcherLamaService-001
    @Test
    void inpaint_Success() {
        MockMultipartFile file = new MockMultipartFile("image", "test.png", MediaType.IMAGE_PNG_VALUE, "dummy content".getBytes());
        byte[] responseBody = "processed image content".getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        ResponseEntity<byte[]> responseEntity = ResponseEntity.ok().headers(headers).body(responseBody);

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
            when(responseSpec.toEntity(byte[].class)).thenReturn(responseEntity);

            ProcessedImage result = service.inpaint(file);
            assertNotNull(result);
            assertArrayEquals(responseBody, result.content());
            assertEquals(MediaType.IMAGE_PNG, result.contentType());
        }
    }

    // TC-UNIT-ResearcherLamaService-002
    @Test
    void inpaint_ThrowsBadRequestException_WhenImageIsNull() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.inpaint(null));
        assertEquals("An image file is required.", exception.getMessage());
    }

    // TC-UNIT-ResearcherLamaService-003
    @Test
    void inpaint_ThrowsBadRequestException_WhenImageIsEmpty() {
        MockMultipartFile file = new MockMultipartFile("image", "test.png", MediaType.IMAGE_PNG_VALUE, new byte[0]);
        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.inpaint(file));
        assertEquals("An image file is required.", exception.getMessage());
    }

    // TC-UNIT-ResearcherLamaService-004
    @Test
    void inpaint_ThrowsFileStorageException_WhenBackendReturnsEmptyImage() {
        MockMultipartFile file = new MockMultipartFile("image", "test.png", MediaType.IMAGE_PNG_VALUE, "dummy content".getBytes());
        ResponseEntity<byte[]> responseEntity = ResponseEntity.ok().body(new byte[0]);

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
            when(responseSpec.toEntity(byte[].class)).thenReturn(responseEntity);

            FileStorageException exception = assertThrows(FileStorageException.class, () -> service.inpaint(file));
            assertEquals("LaMa backend returned an empty image.", exception.getMessage());
        }
    }

    // TC-UNIT-ResearcherLamaService-005
    @Test
    void inpaint_ThrowsFileStorageException_WhenRestClientExceptionThrown() {
        MockMultipartFile file = new MockMultipartFile("image", "test.png", MediaType.IMAGE_PNG_VALUE, "dummy content".getBytes());

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

            FileStorageException exception = assertThrows(FileStorageException.class, () -> service.inpaint(file));
            assertEquals("Unable to connect to the LaMa image processing backend.", exception.getMessage());
        }
    }
}
