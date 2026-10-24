package com.securegate.authentication.service;

import com.securegate.authentication.domain.RefreshToken;
import com.securegate.authentication.domain.Session;
import com.securegate.authentication.repository.RefreshTokenRepository;
import com.securegate.common.SecurityUtils;
import com.securegate.common.error.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private SessionService sessionService;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, sessionService, 604800L);
    }

    @Test
    @DisplayName("Should rotate refresh token on valid request")
    void testRotateRefreshTokenSuccess() {
        String rawToken = "valid-raw-refresh-token-1234567890";
        String tokenHash = SecurityUtils.sha256Hex(rawToken);

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID family = UUID.randomUUID();

        RefreshToken existing = new RefreshToken(tokenHash, sessionId, userId, family, Instant.now().plusSeconds(3600));
        Session session = new Session(userId, "Browser", "127.0.0.1", Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(existing));
        when(sessionService.getSession(sessionId)).thenReturn(Optional.of(session));

        RefreshTokenService.RotatedTokenPair rotated = refreshTokenService.rotateRefreshToken(rawToken);

        assertThat(rotated).isNotNull();
        assertThat(rotated.userId()).isEqualTo(userId);
        assertThat(rotated.newRawRefreshToken()).isNotEqualTo(rawToken);
        assertThat(existing.isConsumed()).isTrue();

        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
        verify(sessionService).touchSession(sessionId);
    }

    @Test
    @DisplayName("Should detect reuse of already-consumed token and revoke entire token family")
    void testReuseDetectionRevokesFamily() {
        String rawToken = "reused-stolen-token-1234567890";
        String tokenHash = SecurityUtils.sha256Hex(rawToken);

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID family = UUID.randomUUID();

        RefreshToken consumedToken = new RefreshToken(tokenHash, sessionId, userId, family, Instant.now().plusSeconds(3600));
        consumedToken.markConsumed(); // Token was already used previously!

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(consumedToken));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rawToken))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Token reuse detected");

        // Verify entire family was revoked!
        verify(refreshTokenRepository).revokeTokenFamily(family);
        verify(sessionService).revokeSession(sessionId);
    }
}
