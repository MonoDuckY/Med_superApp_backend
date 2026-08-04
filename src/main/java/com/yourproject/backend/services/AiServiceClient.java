package com.yourproject.backend.services;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import java.io.IOException;

@Service
public class AiServiceClient {

    private final WebClient webClient;

    public AiServiceClient() {
        // Assume Python AI Service runs on localhost:8000
        this.webClient = WebClient.create("http://localhost:8000");
    }

    /**
     * Gửi ảnh siêu âm sang Python (Endpoint chẩn đoán Clinical - UC-27)
     */
    public String analyzeUltrasound(MultipartFile file, String patientId) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });
        body.add("patient_id", patientId);

        return webClient.post()
                .uri("/api/v1/ai/analyze-ultrasound")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(String.class)
                .block(); // Block since our Spring Boot app is mostly sync (WebMVC)
    }

    /**
     * Gửi Dataset sang Python (Endpoint Batch Preprocess - UC-23)
     */
    public String preprocessDataset(MultipartFile zipFile, String webhookUrl, String optionsJson) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(zipFile.getBytes()) {
            @Override
            public String getFilename() {
                return zipFile.getOriginalFilename();
            }
        });
        body.add("webhook_url", webhookUrl);
        body.add("options", optionsJson);

        return webClient.post()
                .uri("/api/v1/ai/research/preprocess-dataset")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
