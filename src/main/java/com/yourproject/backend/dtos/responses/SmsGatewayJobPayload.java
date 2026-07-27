package com.yourproject.backend.dtos.responses;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data @AllArgsConstructor public class SmsGatewayJobPayload { private String jobId; private String phoneNumber; private String content; }
