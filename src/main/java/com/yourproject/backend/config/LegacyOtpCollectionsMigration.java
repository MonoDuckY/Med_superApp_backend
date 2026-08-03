package com.yourproject.backend.config;

import java.util.List;

import org.bson.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.mongodb.client.MongoCollection;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LegacyOtpCollectionsMigration implements CommandLineRunner {
    private static final String SHARED_COLLECTION = "patient_otps";
    private static final String LEGACY_PASSWORD_RESET_COLLECTION = "password_reset_otps";

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        MongoCollection<Document> sharedOtps = mongoTemplate.getCollection(SHARED_COLLECTION);
        sharedOtps.updateMany(
                new Document("purpose", new Document("$exists", false)),
                new Document("$set", new Document("purpose", "PATIENT_LOGIN")));

        if (!mongoTemplate.collectionExists(LEGACY_PASSWORD_RESET_COLLECTION)) {
            return;
        }

        MongoCollection<Document> legacyOtps = mongoTemplate.getCollection(LEGACY_PASSWORD_RESET_COLLECTION);
        List<Document> documents = legacyOtps.find().into(new java.util.ArrayList<>());
        for (Document document : documents) {
            document.put("purpose", "PASSWORD_RESET");
            if (sharedOtps.countDocuments(new Document("_id", document.get("_id"))) == 0) {
                sharedOtps.insertOne(document);
            }
        }
        mongoTemplate.dropCollection(LEGACY_PASSWORD_RESET_COLLECTION);
    }
}
