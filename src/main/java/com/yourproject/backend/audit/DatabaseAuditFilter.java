package com.yourproject.backend.audit;

import java.io.IOException;
import java.time.Instant;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabaseAuditFilter extends OncePerRequestFilter {
    private final AuditLogService auditLogService;
    private final BackendInstanceIdentity backendIdentity;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return switch (request.getMethod()) {
            case "POST", "PUT", "PATCH", "DELETE" -> false;
            default -> true;
        };
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            auditLogService.save(AuditLog.builder()
                    .timestamp(Instant.now())
                    .actorUserId(authentication == null ? null : authentication.getName())
                    .actorRole(resolveRole(authentication))
                    .action(resolveAction(request.getMethod()))
                    .requestMethod(request.getMethod())
                    .requestPath(request.getRequestURI())
                    .queryString(request.getQueryString())
                    .statusCode(response.getStatus())
                    .durationMs((System.nanoTime() - startedAt) / 1_000_000)
                    .clientIp(resolveClientIp(request))
                    .backendInstanceId(backendIdentity.getInstanceId())
                    .backendHostName(backendIdentity.getHostName())
                    .backendIp(backendIdentity.getIpAddress())
                    .build());
        }
    }

    private String resolveRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().isEmpty()) return null;
        return authentication.getAuthorities().iterator().next().getAuthority().replaceFirst("^ROLE_", "");
    }

    private String resolveAction(String method) {
        return switch (method) {
            case "POST" -> "CREATE_OR_COMMAND";
            case "PUT", "PATCH" -> "UPDATE_OR_COMMAND";
            case "DELETE" -> "DELETE";
            default -> method;
        };
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) return forwardedFor.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
