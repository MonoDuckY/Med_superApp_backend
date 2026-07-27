package com.yourproject.backend.controllers;

import com.yourproject.backend.dtos.requests.FcmSmsCommandRequest;
import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.services.FcmGatewayService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/sms-gateway")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSmsGatewayController {

    private final FcmGatewayService fcmGatewayService;

    @PostMapping("/send-test")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendTestSms(
            @Valid @RequestBody FcmSmsCommandRequest request) {
        String firebaseMessageId = fcmGatewayService.sendSmsCommand(
                request.getGatewayFcmToken(),
                request.getPhoneNumber(),
                request.getContent());
        return ResponseEntity.ok(ApiResponse.success(
                "SMS gateway command sent to Firebase.",
                Map.of("firebaseMessageId", firebaseMessageId)));
    }
}
