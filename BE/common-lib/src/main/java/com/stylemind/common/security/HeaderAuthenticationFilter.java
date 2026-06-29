package com.stylemind.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates a downstream request from the identity headers the API gateway
 * injects after it validates the JWT ({@code X-User-Id}, {@code X-User-Roles},
 * {@code X-User-Email}). Service-to-service Feign calls propagate the same
 * headers (see FeignClientConfig), so this single filter covers both the
 * browser→gateway and service→service paths.
 *
 * <p>Downstream services trust these headers because they live behind the
 * gateway: the gateway strips any client-supplied {@code X-User-*} headers
 * before forwarding, and the service ports are not published outside the mesh.
 * {@code /internal/**} endpoints are guarded separately by
 * {@link InternalAuthFilter} and are skipped here.
 */
@Component
@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLES_HEADER = "X-User-Roles";
    private static final String USER_EMAIL_HEADER = "X-User-Email";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/internal/") || path.startsWith("/actuator/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = request.getHeader(USER_ID_HEADER);

        if (StringUtils.hasText(userId)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String rolesHeader = request.getHeader(USER_ROLES_HEADER); // e.g. "ROLE_ADMIN"
                String email = request.getHeader(USER_EMAIL_HEADER);
                String role = normalizeRole(rolesHeader);

                UserPrincipal principal = new UserPrincipal(userId, email, null, role, null, true);

                List<SimpleGrantedAuthority> authorities = StringUtils.hasText(rolesHeader)
                        ? List.of(new SimpleGrantedAuthority(rolesHeader))
                        : List.of();

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (Exception ex) {
                log.warn("Failed to build authentication from gateway headers: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /** Strips the Spring {@code ROLE_} prefix so the stored role matches the raw value (e.g. "ADMIN"). */
    private String normalizeRole(String rolesHeader) {
        if (!StringUtils.hasText(rolesHeader)) {
            return null;
        }
        return rolesHeader.startsWith("ROLE_") ? rolesHeader.substring("ROLE_".length()) : rolesHeader;
    }
}
