package com.stylemind.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectProvider<UserDetailsService> userDetailsServiceProvider;

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
            final String userEmail = jwtUtil.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetailsService userDetailsService = userDetailsServiceProvider.getIfAvailable();
                if (userDetailsService == null) {
                    log.debug("No UserDetailsService configured; skipping JWT authentication for {}", path);
                    filterChain.doFilter(request, response);
                    return;
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

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
            log.warn("JWT validation failed: {}", ex.getMessage());
            // Don't throw exception, let the request continue (endpoint may be public)
            // The endpoint's @PreAuthorize or manual check will handle authorization
        }

        filterChain.doFilter(request, response);
    }
}
