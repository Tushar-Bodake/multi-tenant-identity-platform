package com.securegate.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingFilterTest {

    private final RateLimitingFilter filter = new RateLimitingFilter();

    @Test
    @DisplayName("Should allow requests within limit and reject with 429 once threshold exceeded")
    void testRateLimitEnforcement() throws Exception {
        String clientIp = "192.168.1.100";

        // Login endpoint has limit of 10 requests per minute
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(req, res, chain);
            assertThat(res.getStatus()).isEqualTo(200);
        }

        // 11th request: Exceeds bucket capacity!
        MockHttpServletRequest reqExceeded = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        reqExceeded.setRemoteAddr(clientIp);
        MockHttpServletResponse resExceeded = new MockHttpServletResponse();
        MockFilterChain chainExceeded = new MockFilterChain();

        filter.doFilter(reqExceeded, resExceeded, chainExceeded);

        assertThat(resExceeded.getStatus()).isEqualTo(429);
        assertThat(resExceeded.getHeader("Retry-After")).isEqualTo("60");
        assertThat(resExceeded.getContentAsString()).contains("Rate limit exceeded");
    }
}
