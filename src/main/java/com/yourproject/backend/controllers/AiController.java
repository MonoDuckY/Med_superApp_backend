package com.yourproject.backend.controllers;

import com.yourproject.backend.services.AiServiceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*") // Cho phép Next.js gọi vào
public class AiController {

    private static final Logger logger = LoggerFactory.getLogger(AiController.class);

    @Autowired
    private AiServiceClient aiServiceClient;

    /**
     * Endpoint chẩn đoán 1 ảnh siêu âm (UC-27)
     */
    @PostMapping("/diagnose")
    public ResponseEntity<?> diagnoseUltrasound(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "patientId", defaultValue = "Unknown") String patientId) {
        try {
            String jsonResponse = aiServiceClient.analyzeUltrasound(file, patientId);
            // Trả thẳng chuỗi JSON của Python về cho Frontend Next.js (có thể parse lại thành DTO nếu cần)
            return ResponseEntity.ok(jsonResponse);
        } catch (Exception e) {
            logger.error("Lỗi khi gọi AI Service: ", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Endpoint đẩy dữ liệu vào Background Preprocess (UC-23)
     */
    @PostMapping("/research/preprocess")
    public ResponseEntity<?> preprocessDataset(
            @RequestParam("file") MultipartFile zipFile,
            @RequestParam(value = "options", defaultValue = "{}") String options) {
        try {
            // URL Webhook trỏ ngược về endpoint /webhooks/ai-job-completed của Spring Boot
            String webhookUrl = "http://localhost:8080/api/webhooks/ai-job-completed";
            String jsonResponse = aiServiceClient.preprocessDataset(zipFile, webhookUrl, options);
            return ResponseEntity.ok(jsonResponse);
        } catch (Exception e) {
            logger.error("Lỗi khi gọi Batch Preprocess: ", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
