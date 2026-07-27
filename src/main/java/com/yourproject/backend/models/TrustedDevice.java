package com.yourproject.backend.models;
import java.time.Instant;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
@Document(collection="trusted_devices") @CompoundIndex(name="user_device_unique",def="{'userId':1,'deviceId':1}",unique=true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder public class TrustedDevice { @Id private String id; private String userId; private String deviceId; private Instant verifiedAt; private Instant revokedAt; }
