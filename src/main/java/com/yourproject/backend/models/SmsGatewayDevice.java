package com.yourproject.backend.models;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "sms_gateway_devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsGatewayDevice {
    @Id
    private String id;
    @Indexed(unique = true)
    private String fcmToken;
    private String deviceName;
    private Instant lastSeenAt;
}
