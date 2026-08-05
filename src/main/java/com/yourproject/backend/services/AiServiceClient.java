package com.yourproject.backend.services;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Value;

@Service
public class AiServiceClient {

    private final WebClient webClient;

    public AiServiceClient(@Value("${ai.service.url:http://127.0.0.1:8000}") String aiServiceUrl) {
        this.webClient = WebClient.create(aiServiceUrl);
    }

    public Mono<String> analyzeUltrasound(MultipartFile file, String patientId) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", file.getResource());
        builder.part("patient_id", patientId != null ? patientId : "Unknown");

        return webClient.post()
                .uri("/api/v1/ai/analyze-ultrasound")
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> batchProcessDataset(MultipartFile file, String options, String webhookUrl) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", file.getResource());
        builder.part("options", options != null ? options : "{}");
        builder.part("webhook_url", webhookUrl != null ? webhookUrl : "");

        return webClient.post()
                .uri("/api/v1/ai/research/preprocess-dataset")
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(String.class);
    }
}
