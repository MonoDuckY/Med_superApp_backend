package com.yourproject.backend.repositories;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.yourproject.backend.models.SmsGatewayDevice;

public interface SmsGatewayDeviceRepository extends MongoRepository<SmsGatewayDevice, String> {
    Optional<SmsGatewayDevice> findByFcmToken(String fcmToken);
    Optional<SmsGatewayDevice> findTopByOrderByLastSeenAtDesc();
}
