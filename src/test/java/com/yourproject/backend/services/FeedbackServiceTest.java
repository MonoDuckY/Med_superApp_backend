package com.yourproject.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yourproject.backend.dtos.requests.CreateFeedbackRequest;
import com.yourproject.backend.dtos.requests.RespondFeedbackRequest;
import com.yourproject.backend.models.Feedback;
import com.yourproject.backend.repositories.FeedbackRepository;
import com.yourproject.backend.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {
    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientDataProtectionService patientDataProtectionService;

    @InjectMocks
    private FeedbackService service;

    @Test
    void patientSubmitsFeedbackWithoutAppointmentCondition() {
        CreateFeedbackRequest request = request(5, "OUTPATIENT_EXAM");
        when(feedbackRepository.save(any(Feedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.submit("patient-1", request);

        assertEquals("patient-1", result.getSenderId());
        assertEquals("OUTPATIENT_EXAM", result.getServiceType());
        assertEquals("SUBMITTED", result.getStatus());
    }

    @Test
    void allowsAnotherFeedbackWhenTheDocDoesNotDefineDuplicateRestriction() {
        when(feedbackRepository.save(any(Feedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("SUBMITTED", service.submit("patient-1", request(4, "EXAM")).getStatus());
    }

    @Test
    void staffRespondsAndUpdatesFeedbackStatus() {
        Feedback feedback = Feedback.builder()
                .feedbackId("feedback-1")
                .senderId("patient-1")
                .rating(4)
                .serviceType("EXAM")
                .status("SUBMITTED")
                .build();
        when(feedbackRepository.findById("feedback-1")).thenReturn(Optional.of(feedback));
        when(feedbackRepository.save(feedback)).thenReturn(feedback);
        RespondFeedbackRequest request = new RespondFeedbackRequest();
        request.setResponse("Thank you for your feedback.");

        var result = service.respond("staff-1", "feedback-1", request);

        assertEquals("staff-1", result.getReceiverId());
        assertEquals("RESPONDED", result.getStatus());
        assertEquals("Thank you for your feedback.", result.getResponse());
        verify(feedbackRepository).save(feedback);
    }

    private CreateFeedbackRequest request(int rating, String serviceType) {
        CreateFeedbackRequest request = new CreateFeedbackRequest();
        request.setRating(rating);
        request.setServiceType(serviceType);
        request.setContent("Good service.");
        return request;
    }
}
