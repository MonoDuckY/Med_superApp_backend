package com.yourproject.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class AiWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(AiWebhookController.class);

    /**
     * Webhook nhận tín hiệu hoàn tất từ Background Tasks của Python (UC-23, UC-24)
     */
    @PostMapping("/ai-job-completed")
    public ResponseEntity<?> handleAiJobCompleted(@RequestBody Map<String, Object> payload) {
        logger.info("Nhận được Webhook từ AI Service!");
        logger.info("Payload: {}", payload);

        // TODO: Cập nhật trạng thái Job vào Database (MongoDB/PostgreSQL) 
        // và lưu URL file kết quả để Next.js/Researcher tải xuống.
        String jobId = (String) payload.get("job_id");
        String status = (String) payload.get("status");

        if ("success".equals(status)) {
            logger.info("Job {} đã thành công!", jobId);
        } else {
            logger.error("Job {} đã thất bại!", jobId);
        }

        return ResponseEntity.ok(Map.of("message", "Đã nhận webhook thành công."));
    }
}
