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

import com.mongodb.client.result.UpdateResult;
import com.yourproject.backend.models.DoctorWorkSlot;

@Component
@Order(3)
public class DoctorWorkSlotLegacyFieldsMigration implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DoctorWorkSlotLegacyFieldsMigration.class);

    private final MongoTemplate mongoTemplate;

    public DoctorWorkSlotLegacyFieldsMigration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Query legacyDocuments = Query.query(new Criteria().orOperator(
                Criteria.where("startAt").exists(true),
                Criteria.where("endAt").exists(true),
                Criteria.where("createdAt").exists(true)));
        Update removeLegacyTimes = new Update()
                .unset("startAt")
                .unset("endAt")
                .unset("createdAt");
        UpdateResult result = mongoTemplate.updateMulti(
                legacyDocuments,
                removeLegacyTimes,
                DoctorWorkSlot.class);
        if (result.getModifiedCount() > 0) {
            LOGGER.info("Removed legacy startAt/endAt/createdAt from {} doctor work slots.",
                    result.getModifiedCount());
        }
    }
}
