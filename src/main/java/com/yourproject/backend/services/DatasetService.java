package com.yourproject.backend.services;

import com.yourproject.backend.models.Dataset;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface DatasetService {
    Dataset createDataset(String name, String description, String researcherId);
    List<Dataset> getDatasetsByResearcher(String researcherId);
    Dataset getDatasetById(String id);
    Dataset uploadRawZip(String id, MultipartFile zipFile);
    void triggerPreProcessing(String id);
    void handlePreProcessingWebhook(String id, String processedZipUrl);
}
