package com.securegate.authentication.service;

import com.securegate.audit.service.AuditService;
import com.securegate.authentication.domain.MagicLinkToken;
import com.securegate.authentication.domain.Session;
import com.securegate.authentication.repository.MagicLinkTokenRepository;
import com.securegate.common.SecurityUtils;
import com.securegate.common.error.ConflictException;
import com.securegate.common.error.ResourceNotFoundException;
import com.securegate.common.error.UnauthorizedException;
import com.securegate.identity.domain.User;
import com.securegate.identity.domain.UserStatus;
import com.securegate.identity.dto.AuthResponse;
import com.securegate.identity.dto.UserDto;
import com.securegate.identity.repository.UserRepository;
import com.securegate.notification.EmailService;
import com.securegate.tenant.domain.OrganizationMembership;
import com.securegate.tenant.dto.OrganizationDto;
import com.securegate.tenant.service.TenantService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class MagicLinkService {

    private final MagicLinkTokenRepository magicLinkTokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final SessionService sessionService;
    private final TenantService tenantService;
    private final AuditService auditService;
    private final int expirationMinutes;

    public MagicLinkService(
            MagicLinkTokenRepository magicLinkTokenRepository,
            UserRepository userRepository,
            EmailService emailService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            SessionService sessionService,
            TenantService tenantService,
            AuditService auditService,
            @Value("${securegate.magic-link.expiration-minutes:15}") int expirationMinutes) {
        this.magicLinkTokenRepository = magicLinkTokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.sessionService = sessionService;
        this.tenantService = tenantService;
        this.auditService = auditService;
        this.expirationMinutes = expirationMinutes;
    }

    @Transactional
    public void sendMagicLink(String email) {
        String normalizedEmail = SecurityUtils.normalizeEmail(email);

        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            String rawToken = SecurityUtils.generateSecureToken(32);
            String tokenHash = SecurityUtils.sha256Hex(rawToken);
            Instant expiresAt = Instant.now().plusSeconds(expirationMinutes * 60L);

            MagicLinkToken token = new MagicLinkToken(normalizedEmail, tokenHash, expiresAt);
            magicLinkTokenRepository.save(token);

            emailService.sendMagicLinkEmail(normalizedEmail, rawToken);

            auditService.logEvent(user.getId(), null, "MAGIC_LINK_REQUEST", "USER",
                    user.getId().toString(), "SUCCESS", UUID.randomUUID().toString(), "{}");
        });
    }

    @Transactional
    public AuthResponse verifyMagicLink(String rawToken, String deviceInfo, String ipAddress, UUID requestedTenantId) {
        String tokenHash = SecurityUtils.sha256Hex(rawToken);
        MagicLinkToken token = magicLinkTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired magic link token"));

        if (!token.isValid()) {
            throw new ConflictException("Magic link has already been used or expired");
        }

        User user = userRepository.findByEmail(token.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }

        token.markUsed();
        magicLinkTokenRepository.save(token);

        List<OrganizationDto> organizations = tenantService.getUserOrganizations(user.getId());
        OrganizationDto activeOrg = null;
        Set<String> permissions = new HashSet<>();

        if (!organizations.isEmpty()) {
            if (requestedTenantId != null) {
                activeOrg = organizations.stream()
                        .filter(o -> o.id().equals(requestedTenantId))
                        .findFirst()
                        .orElse(organizations.get(0));
            } else {
                activeOrg = organizations.get(0);
            }
            OrganizationMembership membership = tenantService.getMembership(activeOrg.id(), user.getId());
            membership.getRole().getPermissions().forEach(p -> permissions.add(p.getName()));
        }

        Session session = sessionService.createSession(user.getId(), deviceInfo, ipAddress, 7 * 24 * 3600);
        String rawRefreshToken = refreshTokenService.issueInitialRefreshToken(user.getId(), session.getId());

        UUID activeOrgId = activeOrg != null ? activeOrg.id() : null;
        String activeRole = activeOrg != null ? activeOrg.role() : null;
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getFullName(),
                activeOrgId, activeRole, permissions);

        auditService.logEvent(user.getId(), activeOrgId, "MAGIC_LINK_LOGIN_SUCCESS", "USER",
                user.getId().toString(), "SUCCESS", UUID.randomUUID().toString(), "{}");

        UserDto userDto = new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.isEmailVerified(), user.getStatus());
        return AuthResponse.of(accessToken, rawRefreshToken, jwtService.getAccessTokenExpirationSeconds(), userDto, activeOrg, organizations);
    }
}
