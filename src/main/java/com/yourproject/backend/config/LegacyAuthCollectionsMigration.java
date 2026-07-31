package com.yourproject.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(4)
public class LegacyAuthCollectionsMigration implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyAuthCollectionsMigration.class);

    private final MongoTemplate mongoTemplate;

    public LegacyAuthCollectionsMigration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        dropLegacyCollection("refresh_tokens");
        dropLegacyCollection("trusted_devices");
    }

    private void dropLegacyCollection(String collectionName) {
        if (mongoTemplate.collectionExists(collectionName)) {
            mongoTemplate.dropCollection(collectionName);
            LOGGER.info("Removed legacy MongoDB collection: {}", collectionName);
        }
    }
}
