package com.yourproject.backend.controllers;
import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.dtos.responses.SmsGatewayJobPayload;
import com.yourproject.backend.services.SmsGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/sms-gateway") @RequiredArgsConstructor
public class SmsGatewayController {
 private final SmsGatewayService service;
 @GetMapping("/jobs/{jobId}") public ResponseEntity<ApiResponse<SmsGatewayJobPayload>> job(@RequestHeader("X-Gateway-Registration-Key") String key,@PathVariable String jobId){return ResponseEntity.ok(ApiResponse.success("SMS job retrieved.",service.getJob(key,jobId)));}
 @PostMapping("/jobs/{jobId}/complete") public ResponseEntity<ApiResponse<Void>> complete(@RequestHeader("X-Gateway-Registration-Key") String key,@PathVariable String jobId,@RequestParam boolean sent,@RequestParam(required=false) String reason){service.complete(key,jobId,sent,reason);return ResponseEntity.ok(ApiResponse.success("SMS job updated.",null));}
}
