package com.yourproject.backend.services;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.yourproject.backend.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FcmGatewayService {

    private final ObjectProvider<FirebaseApp> firebaseAppProvider;

    public String sendSmsCommand(String gatewayFcmToken, String phoneNumber, String content) {
        FirebaseApp firebaseApp = firebaseAppProvider.getIfAvailable();
        if (firebaseApp == null) {
            throw new BadRequestException("Firebase gateway is not configured on this server.");
        }

        Message message = Message.builder()
                .setToken(gatewayFcmToken)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .putData("command", "send_sms")
                .putData("phoneNumber", phoneNumber)
                .putData("content", content)
                .build();

        try {
            return FirebaseMessaging.getInstance(firebaseApp).send(message);
        } catch (FirebaseMessagingException exception) {
            throw new BadRequestException("Firebase could not deliver the gateway command: "
                    + exception.getMessagingErrorCode());
        }
    }

    public void sendJobCommand(String gatewayFcmToken, String jobId) {
        FirebaseApp firebaseApp = firebaseAppProvider.getIfAvailable();
        if (firebaseApp == null) throw new BadRequestException("Firebase gateway is not configured on this server.");
        try {
            FirebaseMessaging.getInstance(firebaseApp).send(Message.builder()
                    .setToken(gatewayFcmToken)
                    .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
                    .putData("command", "fetch_sms_job").putData("jobId", jobId).build());
        } catch (FirebaseMessagingException exception) {
            throw new BadRequestException("Firebase could not deliver the gateway command.");
        }
    }
}
