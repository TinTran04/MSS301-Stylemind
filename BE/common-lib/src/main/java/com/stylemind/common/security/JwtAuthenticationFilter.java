package com.stylemind.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    @Lazy
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Skip JWT validation for internal endpoints (they use InternalAuthFilter)
        String path = request.getRequestURI();
        if (path.startsWith("/internal/") || path.startsWith("/actuator/")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String userId = jwtUtil.extractUserId(jwt);
            final String tokenType = jwtUtil.extractTokenType(jwt);
            final String jti = jwtUtil.extractJti(jwt);

            if (!"access".equals(tokenType) || jti == null || jti.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // Set headers for downstream services (will be propagated by Gateway)
                    if (userDetails instanceof UserPrincipal principal) {
                        request.setAttribute("X-User-Id", principal.getUserId());
                        request.setAttribute("X-User-Roles", "ROLE_" + principal.getRole());
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("JWT validation failed for path={}: {}", path, ex.getClass().getSimpleName());
            // Don't throw exception, let the request continue (endpoint may be public)
            // The endpoint's @PreAuthorize or manual check will handle authorization
        }

        filterChain.doFilter(request, response);
    }
}
