package com.yourproject.backend.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.exceptions.FileStorageException;

@Service
public class ResearcherLamaService {
    private final String lamaBackendUrl;

    public ResearcherLamaService(
            @Value("${app.lama.backend-url:http://18.143.151.200:8000}") String lamaBackendUrl) {
        this.lamaBackendUrl = lamaBackendUrl;
    }

    public ProcessedImage inpaint(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("An image file is required.");
        }

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("image", image.getResource())
                .filename(image.getOriginalFilename() == null ? "image" : image.getOriginalFilename())
                .contentType(image.getContentType() == null
                        ? MediaType.APPLICATION_OCTET_STREAM
                        : MediaType.parseMediaType(image.getContentType()));
        MultiValueMap<String, org.springframework.http.HttpEntity<?>> body = bodyBuilder.build();

        try {
            ResponseEntity<byte[]> response = RestClient.create()
                    .post()
                    .uri(lamaBackendUrl + "/inpaint")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .toEntity(byte[].class);
            byte[] responseBody = response.getBody();
            if (responseBody == null || responseBody.length == 0) {
                throw new FileStorageException("LaMa backend returned an empty image.");
            }
            MediaType contentType = response.getHeaders().getContentType();
            if (contentType == null || !contentType.getType().equalsIgnoreCase("image")) {
                contentType = MediaType.IMAGE_PNG;
            }
            return new ProcessedImage(responseBody, contentType);
        } catch (RestClientException exception) {
            throw new FileStorageException("Unable to connect to the LaMa image processing backend.");
        }
    }

    public record ProcessedImage(byte[] content, MediaType contentType) {
    }
}
