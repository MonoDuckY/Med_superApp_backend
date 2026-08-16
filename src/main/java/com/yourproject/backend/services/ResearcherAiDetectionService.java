package com.yourproject.backend.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.databind.JsonNode;
import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.exceptions.FileStorageException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResearcherAiDetectionService {
    @Value("${app.ai.backend-url:http://127.0.0.1:8000}")
    private String aiBackendUrl;

    public JsonNode detect(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("An image file is required.");
        }

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", image.getResource())
                .filename(image.getOriginalFilename() == null ? "image" : image.getOriginalFilename())
                .contentType(image.getContentType() == null
                        ? MediaType.APPLICATION_OCTET_STREAM
                        : MediaType.parseMediaType(image.getContentType()));
        MultiValueMap<String, org.springframework.http.HttpEntity<?>> body = bodyBuilder.build();

        try {
            JsonNode response = RestClient.create()
                    .post()
                    .uri(aiBackendUrl + "/api/detect")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw new FileStorageException("AI backend returned an empty response.");
            }
            return response;
        } catch (RestClientException exception) {
            throw new FileStorageException("Unable to connect to the AI detection backend.");
        }
    }
}
