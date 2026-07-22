package com.yourproject.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import com.yourproject.backend.models.User;

@Component
@Order(1)
public class UserIndexMigration implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserIndexMigration.class);

    private final MongoTemplate mongoTemplate;

    public UserIndexMigration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        IndexOperations indexOperations = mongoTemplate.indexOps(User.class);
        boolean hasLegacyUsernameIndex = indexOperations.getIndexInfo().stream()
                .anyMatch(index -> "username_1".equals(index.getName()));

        if (hasLegacyUsernameIndex) {
            indexOperations.dropIndex("username_1");
            LOGGER.info("Removed legacy username index from users collection.");
        }
    }
}
