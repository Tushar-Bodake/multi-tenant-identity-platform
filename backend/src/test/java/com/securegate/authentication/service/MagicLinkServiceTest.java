package com.securegate.authentication.service;

import com.securegate.audit.service.AuditService;
import com.securegate.authentication.domain.MagicLinkToken;
import com.securegate.authentication.domain.Session;
import com.securegate.authentication.repository.MagicLinkTokenRepository;
import com.securegate.common.SecurityUtils;
import com.securegate.common.error.ConflictException;
import com.securegate.common.error.ResourceNotFoundException;
import com.securegate.identity.domain.User;
import com.securegate.identity.dto.AuthResponse;
import com.securegate.identity.repository.UserRepository;
import com.securegate.notification.EmailService;
import com.securegate.tenant.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MagicLinkServiceTest {

    @Mock private MagicLinkTokenRepository magicLinkTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private SessionService sessionService;
    @Mock private TenantService tenantService;
    @Mock private AuditService auditService;

    private MagicLinkService magicLinkService;

    @BeforeEach
    void setUp() {
        magicLinkService = new MagicLinkService(
                magicLinkTokenRepository,
                userRepository,
                emailService,
                jwtService,
                refreshTokenService,
                sessionService,
                tenantService,
                auditService,
                15
        );
    }

    @Test
    @DisplayName("Should send magic link token to registered email")
    void testSendMagicLinkSuccess() {
        String email = "alice@example.com";
        User user = new User("Alice", email, null);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        magicLinkService.sendMagicLink(email);

        verify(magicLinkTokenRepository).save(any(MagicLinkToken.class));
        verify(emailService).sendMagicLinkEmail(eq(email), any(String.class));
    }

    @Test
    @DisplayName("Should verify valid magic link token and authenticate user")
    void testVerifyMagicLinkSuccess() {
        String rawToken = "test-raw-token-123";
        String tokenHash = SecurityUtils.sha256Hex(rawToken);
        String email = "alice@example.com";

        MagicLinkToken token = new MagicLinkToken(email, tokenHash, Instant.now().plusSeconds(600));
        User user = new User("Alice", email, null);
        Session session = new Session(user.getId(), "Mozilla", "127.0.0.1", Instant.now().plusSeconds(3600));

        when(magicLinkTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(sessionService.createSession(any(), any(), any(), anyLong())).thenReturn(session);
        when(refreshTokenService.issueInitialRefreshToken(any(), any())).thenReturn("raw-refresh-token");
        when(tenantService.getUserOrganizations(user.getId())).thenReturn(List.of());
        when(jwtService.generateAccessToken(any(), any(), any(), any(), any(), any())).thenReturn("mock-access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        AuthResponse response = magicLinkService.verifyMagicLink(rawToken, "Mozilla", "127.0.0.1", null);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("mock-access-token");
        assertThat(token.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should reject expired or already-used magic link token")
    void testRejectUsedToken() {
        String rawToken = "used-token";
        String tokenHash = SecurityUtils.sha256Hex(rawToken);
        MagicLinkToken token = new MagicLinkToken("alice@example.com", tokenHash, Instant.now().plusSeconds(600));
        token.markUsed();

        when(magicLinkTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> magicLinkService.verifyMagicLink(rawToken, "Mozilla", "127.0.0.1", null))
                .isInstanceOf(ConflictException.class);
    }
}
