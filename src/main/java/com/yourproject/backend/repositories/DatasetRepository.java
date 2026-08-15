package com.yourproject.backend.repositories;

import com.yourproject.backend.models.Dataset;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DatasetRepository extends MongoRepository<Dataset, String> {
    List<Dataset> findByResearcherId(String researcherId);
}
