package com.securegate.authentication.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.securegate.common.error.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "issuer", "https://auth.securegate.test");
        ReflectionTestUtils.setField(jwtService, "audience", "securegate-test");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationSeconds", 300L);
        jwtService.init();
    }

    @Test
    @DisplayName("Should generate valid RS256 JWT access token with required claims")
    void testGenerateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String email = "alice@example.com";
        String fullName = "Alice Doe";
        String role = "ORG_ADMIN";
        Set<String> permissions = Set.of("organization:read", "members:read");

        String token = jwtService.generateAccessToken(userId, email, fullName, tenantId, role, permissions);
        assertThat(token).isNotBlank();

        JWTClaimsSet claims = jwtService.validateAndParseToken(token);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getIssuer()).isEqualTo("https://auth.securegate.test");
        assertThat(claims.getAudience()).contains("securegate-test");
        assertThat(claims.getClaim("email")).isEqualTo(email);
        assertThat(claims.getClaim("name")).isEqualTo(fullName);
        assertThat(claims.getClaim("tenant_id")).isEqualTo(tenantId.toString());
        assertThat(claims.getClaim("role")).isEqualTo(role);
        assertThat(claims.getClaim("permissions")).asList().containsExactlyInAnyOrder("organization:read", "members:read");
    }

    @Test
    @DisplayName("Should reject tampered token")
    void testRejectTamperedToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "bob@example.com", "Bob", null, null, Set.of());

        // Tamper with signature payload
        String tampered = token.substring(0, token.length() - 5) + "abcde";

        assertThatThrownBy(() -> jwtService.validateAndParseToken(tampered))
                .isInstanceOf(UnauthorizedException.class);
    }
}
