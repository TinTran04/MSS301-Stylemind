package com.stylemind.common.security;

import com.stylemind.common.exception.BusinessException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class InternalAuthFilterTest {

    private InternalAuthFilter filterWithToken(String configuredToken) {
        InternalAuthFilter filter = new InternalAuthFilter();
        ReflectionTestUtils.setField(filter, "internalToken", configuredToken);
        return filter;
    }

    @Test
    void validInternalToken_isAcceptedWithoutRequiringAUserJwt() throws Exception {
        InternalAuthFilter filter = filterWithToken("configured-token");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/v1/notifications/email");
        request.addHeader("X-Internal-Token", "configured-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void missingInternalToken_isRejected() throws Exception {
        InternalAuthFilter filter = filterWithToken("configured-token");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/v1/notifications/email");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, chain))
                .isInstanceOf(BusinessException.class);

        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void wrongInternalToken_isRejected() throws Exception {
        InternalAuthFilter filter = filterWithToken("configured-token");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/v1/notifications/email");
        request.addHeader("X-Internal-Token", "a-different-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, chain))
                .isInstanceOf(BusinessException.class);

        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void nonInternalPath_bypassesTokenCheckEvenWithoutAToken() throws Exception {
        InternalAuthFilter filter = filterWithToken("configured-token");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notifications");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
