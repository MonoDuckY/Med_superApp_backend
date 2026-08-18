package com.yourproject.backend.services;

import org.springframework.beans.factory.annotation.Value;
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
public class ResearcherImageCompareService {
    private final String khoanhBackendUrl;

    public ResearcherImageCompareService(
            @Value("${app.khoanh.backend-url:http://127.0.0.1:8000}") String khoanhBackendUrl) {
        this.khoanhBackendUrl = khoanhBackendUrl;
    }

    public ProcessedImage compare(MultipartFile image1, MultipartFile image2) {
        validateImage(image1, "image1");
        validateImage(image2, "image2");

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        addPart(bodyBuilder, "image1", image1);
        addPart(bodyBuilder, "image2", image2);
        MultiValueMap<String, org.springframework.http.HttpEntity<?>> body = bodyBuilder.build();

        try {
            ResponseEntity<byte[]> response = RestClient.create()
                    .post()
                    .uri(khoanhBackendUrl + "/compare")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .toEntity(byte[].class);
            byte[] responseBody = response.getBody();
            if (responseBody == null || responseBody.length == 0) {
                throw new FileStorageException("Image comparison backend returned an empty image.");
            }
            MediaType contentType = response.getHeaders().getContentType();
            if (contentType == null || !contentType.getType().equalsIgnoreCase("image")) {
                contentType = MediaType.IMAGE_PNG;
            }
            return new ProcessedImage(responseBody, contentType);
        } catch (RestClientException exception) {
            throw new FileStorageException("Unable to connect to the image comparison backend.");
        }
    }

    private void addPart(MultipartBodyBuilder bodyBuilder, String name, MultipartFile file) {
        bodyBuilder.part(name, file.getResource())
                .filename(file.getOriginalFilename() == null ? name : file.getOriginalFilename())
                .contentType(file.getContentType() == null
                        ? MediaType.APPLICATION_OCTET_STREAM
                        : MediaType.parseMediaType(file.getContentType()));
    }

    private void validateImage(MultipartFile image, String name) {
        if (image == null || image.isEmpty()) {
            throw new BadRequestException(name + " image is required.");
        }
    }

    public record ProcessedImage(byte[] content, MediaType contentType) {
    }
}
