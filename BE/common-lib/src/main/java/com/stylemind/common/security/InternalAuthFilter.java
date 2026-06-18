package com.stylemind.common.security;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.common.constant.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class InternalAuthFilter extends OncePerRequestFilter {

    @Value("${internal.token:sm-secret-internal-service-token-key-2026}")
    private String internalToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Only validate internal token for /internal/** endpoints
        if (path.startsWith("/internal/")) {
            String token = request.getHeader("X-Internal-Token");
            if (token == null || !token.equals(internalToken)) {
                throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}