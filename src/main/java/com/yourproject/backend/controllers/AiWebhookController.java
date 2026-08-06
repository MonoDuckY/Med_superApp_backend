package com.yourproject.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import com.yourproject.backend.services.JobTrackingService;

@RestController
@RequestMapping("/api/webhooks")
public class AiWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(AiWebhookController.class);
    
    private final JobTrackingService jobTrackingService;

    public AiWebhookController(JobTrackingService jobTrackingService) {
        this.jobTrackingService = jobTrackingService;
    }

    /**
     * Webhook nhận tín hiệu hoàn tất từ Background Tasks của Python (UC-23, UC-24)
     */
    @PostMapping("/ai-job-completed")
    public ResponseEntity<?> handleAiJobCompleted(@RequestBody Map<String, Object> payload) {
        String jobId = (String) payload.get("job_id");
        String status = (String) payload.get("status");
        
        if ("processing".equals(status)) {
            // Không log để tránh loãng console, chỉ lưu status
            jobTrackingService.updateJobStatus(jobId, payload);
        } else {
            logger.info("Nhận được Webhook từ AI Service!");
            logger.info("Payload: {}", payload);

            if ("success".equals(status)) {
                logger.info("Job {} đã thành công!", jobId);
                jobTrackingService.updateJobStatus(jobId, payload);
            } else {
                logger.error("Job {} đã thất bại!", jobId);
                jobTrackingService.updateJobStatus(jobId, payload);
            }
        }

        return ResponseEntity.ok(Map.of("message", "Đã nhận webhook thành công."));
    }
}
