package com.stylemind.common.security;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.constant.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class InternalAuthFilter extends OncePerRequestFilter {

    @Value("${internal.token:${INTERNAL_SERVICE_SECRET:${INTERNAL_TOKEN:change-me-local-internal-service-secret}}}")
    private String internalToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/internal/") || isInternalRequest(request)) {
            if (!hasValidInternalToken(request)) {
                throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
            }
            authenticateInternalUser(request);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isInternalRequest(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader("X-Internal-Request"))
                || StringUtils.hasText(request.getHeader("X-User-Id"))
                || StringUtils.hasText(request.getHeader("X-User-Role"))
                || StringUtils.hasText(request.getHeader("X-User-Roles"));
    }

    private boolean hasValidInternalToken(HttpServletRequest request) {
        String token = request.getHeader("X-Internal-Token");
        if (!StringUtils.hasText(token)) {
            token = request.getHeader("X-Internal-Service-Secret");
        }
        return StringUtils.hasText(internalToken) && internalToken.equals(token);
    }

    private void authenticateInternalUser(HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        String userId = request.getHeader("X-User-Id");
        if (!StringUtils.hasText(userId)) {
            return;
        }

        String role = request.getHeader("X-User-Role");
        if (!StringUtils.hasText(role)) {
            role = request.getHeader("X-User-Roles");
        }
        String normalizedRole = normalizeRole(role);
        UserPrincipal principal = new UserPrincipal(userId, userId, "", normalizedRole, "INTERNAL", true);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "CUSTOMER";
        }
        String normalized = role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized.substring("ROLE_".length()) : normalized;
    }
}
