package com.securegate.authentication.service;

import com.securegate.authentication.domain.RefreshToken;
import com.securegate.authentication.domain.Session;
import com.securegate.authentication.repository.RefreshTokenRepository;
import com.securegate.common.SecurityUtils;
import com.securegate.common.error.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionService sessionService;
    private final long refreshTokenExpirationSeconds;

    public record RotatedTokenPair(String newRawRefreshToken, UUID userId, UUID sessionId) {}

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            SessionService sessionService,
            @Value("${securegate.jwt.refresh-token-expiration-seconds:604800}") long refreshTokenExpirationSeconds) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.sessionService = sessionService;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    @Transactional
    public String issueInitialRefreshToken(UUID userId, UUID sessionId) {
        String rawToken = SecurityUtils.generateSecureToken(32);
        String tokenHash = SecurityUtils.sha256Hex(rawToken);
        UUID tokenFamily = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(refreshTokenExpirationSeconds);

        RefreshToken refreshToken = new RefreshToken(tokenHash, sessionId, userId, tokenFamily, expiresAt);
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional
    public RotatedTokenPair rotateRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        String tokenHash = SecurityUtils.sha256Hex(rawRefreshToken);
        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        // REUSE DETECTION: If an already-consumed token is presented again, compromise has occurred!
        if (existingToken.isConsumed()) {
            log.warn("SECURITY ALERT: Refresh token reuse detected for family={} and userId={}. Revoking family!",
                    existingToken.getTokenFamily(), existingToken.getUserId());
            refreshTokenRepository.revokeTokenFamily(existingToken.getTokenFamily());
            sessionService.revokeSession(existingToken.getSessionId());
            throw new UnauthorizedException("Token reuse detected. All related sessions have been revoked for your security.");
        }

        if (existingToken.isRevoked() || !existingToken.isValid()) {
            throw new UnauthorizedException("Refresh token has expired or been revoked");
        }

        // Verify session is still valid
        Session session = sessionService.getSession(existingToken.getSessionId())
                .orElseThrow(() -> new UnauthorizedException("Associated session not found"));
        if (!session.isValid()) {
            throw new UnauthorizedException("Associated session has expired or been revoked");
        }

        // Consume old token
        existingToken.markConsumed();
        refreshTokenRepository.save(existingToken);

        // Issue new rotated token in the SAME family
        String newRawToken = SecurityUtils.generateSecureToken(32);
        String newTokenHash = SecurityUtils.sha256Hex(newRawToken);
        Instant newExpiresAt = Instant.now().plusSeconds(refreshTokenExpirationSeconds);

        RefreshToken newRefreshToken = new RefreshToken(
                newTokenHash,
                existingToken.getSessionId(),
                existingToken.getUserId(),
                existingToken.getTokenFamily(),
                newExpiresAt
        );
        refreshTokenRepository.save(newRefreshToken);

        // Touch session activity
        sessionService.touchSession(existingToken.getSessionId());

        return new RotatedTokenPair(newRawToken, existingToken.getUserId(), existingToken.getSessionId());
    }

    @Transactional
    public void revokeToken(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = SecurityUtils.sha256Hex(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.revoke();
                refreshTokenRepository.save(token);
                sessionService.revokeSession(token.getSessionId());
            });
        }
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId);
        sessionService.revokeAllUserSessions(userId);
    }
}
