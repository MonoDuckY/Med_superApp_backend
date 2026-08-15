package com.yourproject.backend.controllers;

import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.models.Dataset;
import com.yourproject.backend.services.DatasetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/research/datasets")
@RequiredArgsConstructor
public class ResearcherDatasetController {

    private final DatasetService datasetService;

    @PostMapping
    public ResponseEntity<ApiResponse<Dataset>> createDataset(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String researcherId) {
        Dataset dataset = datasetService.createDataset(name, description, researcherId);
        return ResponseEntity.ok(ApiResponse.success("Dataset created successfully", dataset));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Dataset>>> getDatasets(@RequestParam String researcherId) {
        List<Dataset> datasets = datasetService.getDatasetsByResearcher(researcherId);
        return ResponseEntity.ok(ApiResponse.success("Fetched datasets successfully", datasets));
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<ApiResponse<Dataset>> uploadDatasetZip(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) {
        Dataset dataset = datasetService.uploadRawZip(id, file);
        return ResponseEntity.ok(ApiResponse.success("Dataset zip uploaded successfully", dataset));
    }

    @PostMapping("/{id}/preprocess")
    public ResponseEntity<ApiResponse<String>> triggerPreProcessing(@PathVariable String id) {
        datasetService.triggerPreProcessing(id);
        return ResponseEntity.ok(ApiResponse.success("Preprocessing triggered successfully", null));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestParam String datasetId,
            @RequestBody java.util.Map<String, Object> payload) {
        String processedZipUrl = (String) payload.get("download_url");
        if ("success".equals(payload.get("status")) && processedZipUrl != null) {
            datasetService.handlePreProcessingWebhook(datasetId, processedZipUrl);
        }
        return ResponseEntity.ok().build();
    }
}
