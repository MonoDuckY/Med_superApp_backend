package com.yourproject.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;

@Component
@Order(1)
public class UserRoleIdMigration implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRoleIdMigration.class);

    private final MongoTemplate mongoTemplate;

    public UserRoleIdMigration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (UserRole role : UserRole.values()) {
            long updated = mongoTemplate.updateMulti(
                    Query.query(Criteria.where("roleId").is(role.name())),
                    Update.update("roleId", role.getId()),
                    User.class).getModifiedCount();
            if (updated > 0) {
                LOGGER.info("Migrated {} user roleId values from {} to {}.", updated, role.name(), role.getId());
            }
        }
    }
}
