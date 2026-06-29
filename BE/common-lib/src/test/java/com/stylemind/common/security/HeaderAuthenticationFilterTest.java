package com.stylemind.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class HeaderAuthenticationFilterTest {

    private HeaderAuthenticationFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new HeaderAuthenticationFilter();
        chain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @Test
    void setsAuthenticationFromGatewayHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-User-Id", "user-123");
        request.addHeader("X-User-Roles", "ROLE_CUSTOMER");
        request.addHeader("X-User-Email", "alice@example.com");

        filter.doFilterInternal(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipal.class);

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertThat(principal.getUserId()).isEqualTo("user-123");
        assertThat(principal.getRole()).isEqualTo("CUSTOMER");

        // Verify the chain was called with the same request/response objects
        verify(chain).doFilter(request, response);
    }

    @Test
    void skipsAuthenticationWhenUserIdHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(any(), any());
    }

    @Test
    void skipsInternalPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/products/sync");
        request.addHeader("X-User-Id", "user-123");

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(any(), any());
    }

    @Test
    void skipsActuatorPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.addHeader("X-User-Id", "user-123");

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(any(), any());
    }

    @Test
    void stripsRolePrefixForPrincipal() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/ai");
        request.addHeader("X-User-Id", "admin-1");
        request.addHeader("X-User-Roles", "ROLE_ADMIN");

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        assertThat(principal.getRole()).isEqualTo("ADMIN");
        // Authority should still contain the ROLE_ prefix for Spring Security hasRole checks
        assertThat(principal.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
