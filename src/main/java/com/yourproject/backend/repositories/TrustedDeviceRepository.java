package com.yourproject.backend.repositories;
import java.util.Optional; import org.springframework.data.mongodb.repository.MongoRepository; import com.yourproject.backend.models.TrustedDevice;
public interface TrustedDeviceRepository extends MongoRepository<TrustedDevice,String>{ Optional<TrustedDevice> findByUserIdAndDeviceIdAndRevokedAtIsNull(String userId,String deviceId); }
