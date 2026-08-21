package com.yourproject.backend.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.yourproject.backend.dtos.requests.CreateFeedbackRequest;
import com.yourproject.backend.dtos.requests.RespondFeedbackRequest;
import com.yourproject.backend.dtos.responses.FeedbackResponse;
import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.exceptions.ResourceNotFoundException;
import com.yourproject.backend.models.Feedback;
import com.yourproject.backend.repositories.FeedbackRepository;
import com.yourproject.backend.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    private static final String SUBMITTED = "SUBMITTED";
    private static final String RESPONDED = "RESPONDED";

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final PatientDataProtectionService patientDataProtectionService;

    public FeedbackResponse submit(String patientId, CreateFeedbackRequest request) {
        Feedback feedback = Feedback.builder()
                .senderId(patientId)
                .content(request.getContent())
                .status(SUBMITTED)
                .rating(request.getRating())
                .serviceType(request.getServiceType())
                .build();
        return toResponse(feedbackRepository.save(feedback));
    }

    public List<FeedbackResponse> getPatientFeedback(String patientId) {
        return feedbackRepository.findAllBySenderIdOrderByFeedbackIdDesc(patientId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<FeedbackResponse> getAll() {
        return feedbackRepository.findAllByOrderByFeedbackIdDesc().stream().map(this::toResponse).toList();
    }

    public FeedbackResponse get(String feedbackId) {
        return toResponse(find(feedbackId));
    }

    public FeedbackResponse respond(String staffId, String feedbackId, RespondFeedbackRequest request) {
        Feedback feedback = find(feedbackId);
        feedback.setReceiverId(staffId);
        feedback.setResponse(request.getResponse());
        feedback.setStatus(RESPONDED);
        return toResponse(feedbackRepository.save(feedback));
    }

    private Feedback find(String feedbackId) {
        return feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found."));
    }

    private FeedbackResponse toResponse(Feedback feedback) {
        return FeedbackResponse.builder()
                .feedbackId(feedback.getFeedbackId())
                .senderId(feedback.getSenderId())
                .senderName(resolveUserName(feedback.getSenderId()))
                .receiverId(feedback.getReceiverId())
                .receiverName(resolveUserName(feedback.getReceiverId()))
                .content(feedback.getContent())
                .status(feedback.getStatus())
                .rating(feedback.getRating())
                .serviceType(feedback.getServiceType())
                .response(feedback.getResponse())
                .build();
    }

    private String resolveUserName(String userId) {
        if (userId == null || userRepository == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(user -> {
                    if (patientDataProtectionService != null) {
                        patientDataProtectionService.decryptPatientFields(user);
                    }
                    return user.getFullName();
                })
                .orElse(null);
    }
}
