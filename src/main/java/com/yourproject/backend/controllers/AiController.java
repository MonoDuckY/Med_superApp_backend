package com.yourproject.backend.controllers;

import com.yourproject.backend.services.AiServiceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiServiceClient aiServiceClient;

    public AiController(AiServiceClient aiServiceClient) {
        this.aiServiceClient = aiServiceClient;
    }

    @PostMapping("/diagnose")
    public Mono<ResponseEntity<String>> diagnoseUltrasound(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "patient_id", required = false) String patientId) {
        
        return aiServiceClient.analyzeUltrasound(file, patientId)
                .map(response -> ResponseEntity.ok(response))
                .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().body(
                        "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}"
                )));
    }

    @PostMapping("/research/batch")
    public Mono<ResponseEntity<String>> batchProcess(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "options", defaultValue = "{}") String options,
            @RequestParam(value = "webhook_url", required = false) String webhookUrl) {
            
        return aiServiceClient.batchProcessDataset(file, options, webhookUrl)
                .map(response -> ResponseEntity.ok(response))
                .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().body(
                        "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}"
                )));
    }
}
