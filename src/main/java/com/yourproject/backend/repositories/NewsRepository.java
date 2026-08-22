package com.yourproject.backend.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.yourproject.backend.models.News;
import com.yourproject.backend.models.NewsStatus;

public interface NewsRepository extends MongoRepository<News, String> {
    List<News> findAllByStatusOrderByUploadTimeDesc(NewsStatus status);
}
