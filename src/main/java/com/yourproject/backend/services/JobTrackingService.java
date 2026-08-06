package com.yourproject.backend.services;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobTrackingService {
    
    // In-memory cache to track job progress. 
    // Key: Job ID
    // Value: Map containing status details (status, processed, total, download_url, etc.)
    private final Map<String, Map<String, Object>> jobStatuses = new ConcurrentHashMap<>();

    public void updateJobStatus(String jobId, Map<String, Object> statusData) {
        jobStatuses.put(jobId, statusData);
    }

    public Map<String, Object> getJobStatus(String jobId) {
        return jobStatuses.getOrDefault(jobId, Map.of(
            "status", "unknown", 
            "message", "Job ID not found or hasn't started yet"
        ));
    }
}
