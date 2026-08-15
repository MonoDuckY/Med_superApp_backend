package com.yourproject.backend.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class DatabaseAuditFilterTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void writeRequestCapturesActorClientAndBackendIdentity() throws Exception {
        AuditLogService auditLogService = mock(AuditLogService.class);
        BackendInstanceIdentity identity = mock(BackendInstanceIdentity.class);
        when(identity.getInstanceId()).thenReturn("instance-123");
        when(identity.getHostName()).thenReturn("developer-laptop");
        when(identity.getIpAddress()).thenReturn("192.168.1.10");
        DatabaseAuditFilter filter = new DatabaseAuditFilter(auditLogService, identity);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("PATCH");
        when(request.getRequestURI()).thenReturn("/api/doctor/appointments/123/diagnosis");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(response.getStatus()).thenReturn(200);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "doctor-user-id", null, List.of(new SimpleGrantedAuthority("ROLE_DOCTOR"))));

        filter.doFilter(request, response, chain);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).save(captor.capture());
        AuditLog audit = captor.getValue();
        assertEquals("doctor-user-id", audit.getActorUserId());
        assertEquals("DOCTOR", audit.getActorRole());
        assertEquals("instance-123", audit.getBackendInstanceId());
        assertEquals("127.0.0.1", audit.getClientIp());
        assertEquals(200, audit.getStatusCode());
    }
}
