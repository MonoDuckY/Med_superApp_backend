package com.yourproject.backend.services;

import com.yourproject.backend.dtos.responses.SmsGatewayJobPayload;
import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.exceptions.ResourceNotFoundException;
import com.yourproject.backend.models.SmsGatewayJob;
import com.yourproject.backend.models.SmsGatewayJobStatus;
import com.yourproject.backend.repositories.SmsGatewayJobRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class SmsGatewayService {
    private final SmsGatewayJobRepository jobRepository;
    private final PatientDataProtectionService protectionService; private final FcmGatewayService fcmGatewayService;
    @Value("${app.sms-gateway.registration-key}") private String registrationKey;
    @Value("${app.sms-gateway.direct-fcm-token}") private String directFcmToken;
    public void enqueue(String userId, String phone, String content, Instant expiresAt) {
        if (directFcmToken != null && !directFcmToken.isBlank()) {
            fcmGatewayService.sendSmsCommand(directFcmToken, phone, content);
            return;
        }
        throw new BadRequestException("SMS_GATEWAY_DIRECT_FCM_TOKEN is not configured.");
    }
    public SmsGatewayJobPayload getJob(String key, String jobId) {
        requireKey(key); SmsGatewayJob job=jobRepository.findById(jobId).orElseThrow(()->new ResourceNotFoundException("SMS job was not found."));
        if(job.getStatus()!=SmsGatewayJobStatus.PENDING || job.getExpiresAt().isBefore(Instant.now())) throw new BadRequestException("SMS job is no longer available.");
        return new SmsGatewayJobPayload(job.getId(),protectionService.decryptSensitiveValue(job.getEncryptedPhoneNumber()),protectionService.decryptSensitiveValue(job.getEncryptedContent()));
    }
    public void complete(String key,String jobId,boolean sent,String failureReason){ requireKey(key); SmsGatewayJob job=jobRepository.findById(jobId).orElseThrow(()->new ResourceNotFoundException("SMS job was not found.")); job.setStatus(sent?SmsGatewayJobStatus.SENT:SmsGatewayJobStatus.FAILED);job.setCompletedAt(Instant.now());job.setFailureReason(sent?null:failureReason);jobRepository.save(job); }
    private void requireKey(String key){ if(registrationKey==null||registrationKey.isBlank()||key==null||!MessageDigest.isEqual(registrationKey.getBytes(StandardCharsets.UTF_8),key.getBytes(StandardCharsets.UTF_8))) throw new BadRequestException("Invalid SMS gateway key."); }
}
