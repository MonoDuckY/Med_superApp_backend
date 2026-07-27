package com.yourproject.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class AccountStatusMigration implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;

    public AccountStatusMigration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        mongoTemplate.updateMulti(
                new Query(Criteria.where("status").is("DISABLED")),
                new Update().set("status", "INACTIVE"),
                "users");
    }
}
