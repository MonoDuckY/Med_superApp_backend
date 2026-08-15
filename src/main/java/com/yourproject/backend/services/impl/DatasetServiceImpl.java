package com.yourproject.backend.services.impl;

import com.yourproject.backend.models.Dataset;
import com.yourproject.backend.repositories.DatasetRepository;
import com.yourproject.backend.services.DatasetService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatasetServiceImpl implements DatasetService {

    private final DatasetRepository datasetRepository;
    private final RestTemplate restTemplate;

    @Value("${app.ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${app.ai-service.webhook-url:http://localhost:8080/api/v1/research/datasets/webhook}")
    private String webhookUrl;

    @Override
    public Dataset createDataset(String name, String description, String researcherId) {
        Dataset dataset = Dataset.builder()
                .name(name)
                .description(description)
                .researcherId(researcherId)
                .status("CREATED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return datasetRepository.save(dataset);
    }

    @Override
    public List<Dataset> getDatasetsByResearcher(String researcherId) {
        return datasetRepository.findByResearcherId(researcherId);
    }

    @Override
    public Dataset getDatasetById(String id) {
        return datasetRepository.findById(id).orElseThrow(() -> new RuntimeException("Dataset not found"));
    }

    @Override
    public Dataset uploadRawZip(String id, MultipartFile zipFile) {
        Dataset dataset = getDatasetById(id);
        
        try {
            java.io.File tempFile = new java.io.File(System.getProperty("java.io.tmpdir"), "dataset_" + id + ".zip");
            zipFile.transferTo(tempFile);
            
            // Mocking S3 upload
            String mockS3Url = "https://mock-s3-bucket.s3.amazonaws.com/datasets/" + id + "/raw.zip";
            dataset.setRawZipUrl(mockS3Url);
            dataset.setStatus("RAW");
            dataset.setUpdatedAt(LocalDateTime.now());
            
            return datasetRepository.save(dataset);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to store zip file", e);
        }
    }

    @Override
    public void triggerPreProcessing(String id) {
        Dataset dataset = getDatasetById(id);
        if (!"RAW".equals(dataset.getStatus())) {
            throw new RuntimeException("Dataset is not in RAW status");
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            java.io.File tempFile = new java.io.File(System.getProperty("java.io.tmpdir"), "dataset_" + id + ".zip");
            if (!tempFile.exists()) {
                throw new RuntimeException("Raw zip file not found locally");
            }
            org.springframework.core.io.FileSystemResource fileResource = new org.springframework.core.io.FileSystemResource(tempFile);

            body.add("file", fileResource);
            body.add("webhook_url", webhookUrl + "?datasetId=" + id);
            
            String optionsJson = "{" +
                "\"enable_text_removal\": false," +
                "\"enable_srad\": false," +
                "\"enable_safe_area\": false," +
                "\"sharpness\": 0.0," +
                "\"contrast\": 1.0," +
                "\"enable_swinir\": true" +
            "}";
            body.add("options", optionsJson);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            String targetUrl = aiServiceUrl + "/api/v1/ai/research/preprocess-dataset";
            ResponseEntity<String> response = restTemplate.postForEntity(targetUrl, requestEntity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                dataset.setStatus("PREPROCESSING");
                dataset.setUpdatedAt(LocalDateTime.now());
                datasetRepository.save(dataset);
                log.info("Triggered preprocessing for dataset {}", id);
            } else {
                throw new RuntimeException("Failed to trigger preprocessing");
            }
        } catch (Exception e) {
            log.error("Error calling AI service: {}", e.getMessage());
            dataset.setStatus("FAILED");
            datasetRepository.save(dataset);
            throw new RuntimeException("Error communicating with AI Service", e);
        }
    }

    @Override
    public void handlePreProcessingWebhook(String id, String processedZipUrl) {
        Dataset dataset = getDatasetById(id);
        
        dataset.setProcessedZipUrl(processedZipUrl);
        dataset.setStatus("READY");
        dataset.setUpdatedAt(LocalDateTime.now());
        
        datasetRepository.save(dataset);
        log.info("Dataset {} processing completed", id);
    }
}
