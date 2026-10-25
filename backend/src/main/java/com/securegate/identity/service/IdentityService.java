package com.securegate.identity.service;

import com.securegate.audit.service.AuditService;
import com.securegate.authentication.domain.Session;
import com.securegate.authentication.service.JwtService;
import com.securegate.authentication.service.RefreshTokenService;
import com.securegate.authentication.service.SessionService;
import com.securegate.common.SecurityUtils;
import com.securegate.common.error.ConflictException;
import com.securegate.common.error.ResourceNotFoundException;
import com.securegate.common.error.UnauthorizedException;
import com.securegate.identity.domain.*;
import com.securegate.identity.dto.*;
import com.securegate.identity.repository.EmailVerificationTokenRepository;
import com.securegate.identity.repository.PasswordResetTokenRepository;
import com.securegate.identity.repository.UserEmailRepository;
import com.securegate.identity.repository.UserRepository;
import com.securegate.notification.EmailService;
import com.securegate.tenant.domain.Organization;
import com.securegate.tenant.domain.OrganizationMembership;
import com.securegate.tenant.dto.OrganizationDto;
import com.securegate.tenant.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class IdentityService {

    private static final Logger log = LoggerFactory.getLogger(IdentityService.class);

    private final UserRepository userRepository;
    private final UserEmailRepository userEmailRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantService tenantService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final SessionService sessionService;
    private final EmailService emailService;
    private final AuditService auditService;

    public IdentityService(
            UserRepository userRepository,
            UserEmailRepository userEmailRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            TenantService tenantService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            SessionService sessionService,
            EmailService emailService,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.userEmailRepository = userEmailRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantService = tenantService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.sessionService = sessionService;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    @Transactional
    public UserDto register(RegisterRequest request) {
        String normalizedEmail = SecurityUtils.normalizeEmail(request.email());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("An account with this email already exists");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.fullName(), normalizedEmail, passwordHash);
        user = userRepository.save(user);

        UserEmail userEmail = new UserEmail(user.getId(), normalizedEmail, true, false);
        userEmailRepository.save(userEmail);

        // Create user's initial organization
        Organization org = tenantService.createOrganization(request.organizationName(), user);

        // Issue single-use email verification token
        String rawToken = SecurityUtils.generateSecureToken(32);
        String tokenHash = SecurityUtils.sha256Hex(rawToken);
        Instant expiresAt = Instant.now().plusSeconds(24 * 3600); // 24 hours

        EmailVerificationToken verificationToken = new EmailVerificationToken(user.getId(), tokenHash, expiresAt);
        emailVerificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(normalizedEmail, rawToken);

        auditService.logEvent(user.getId(), org.getId(), "USER_REGISTER", "USER",
                user.getId().toString(), "SUCCESS", UUID.randomUUID().toString(),
                "{\"email\":\"" + normalizedEmail + "\"}");

        return new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.isEmailVerified(), user.getStatus());
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        String tokenHash = SecurityUtils.sha256Hex(rawToken);
        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired verification token"));

        if (!token.isValid()) {
            throw new ConflictException("Verification token has already been used or expired");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        token.markUsed();
        emailVerificationTokenRepository.save(token);

        auditService.logEvent(user.getId(), null, "EMAIL_VERIFY", "USER",
                user.getId().toString(), "SUCCESS", UUID.randomUUID().toString(), "{}");
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String deviceInfo, String ipAddress, UUID requestedTenantId) {
        String normalizedEmail = SecurityUtils.normalizeEmail(request.email());

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isAccountNonLocked()) {
            auditService.logEvent(user.getId(), null, "USER_LOGIN_LOCKED", "USER",
                    user.getId().toString(), "DENIED", UUID.randomUUID().toString(), "{}");
            throw new UnauthorizedException("Account is temporarily locked due to repeated failed login attempts. Please try again later.");
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin(5, 15); // Lock after 5 failed attempts for 15 minutes
            userRepository.save(user);

            auditService.logEvent(user.getId(), null, "USER_LOGIN_FAILED", "USER",
                    user.getId().toString(), "FAILURE", UUID.randomUUID().toString(), "{}");
            throw new UnauthorizedException("Invalid email or password");
        }

        // Reset failed logins on success
        user.resetFailedLogin();
        userRepository.save(user);

        // Fetch user organizations
        List<OrganizationDto> organizations = tenantService.getUserOrganizations(user.getId());

        // Resolve active tenant
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

        // Create session
        Session session = sessionService.createSession(user.getId(), deviceInfo, ipAddress, 7 * 24 * 3600);

        // Issue rotated refresh token
        String rawRefreshToken = refreshTokenService.issueInitialRefreshToken(user.getId(), session.getId());

        // Issue RS256 JWT access token
        UUID activeOrgId = activeOrg != null ? activeOrg.id() : null;
        String activeRole = activeOrg != null ? activeOrg.role() : null;
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getFullName(),
                activeOrgId, activeRole, permissions);

        auditService.logEvent(user.getId(), activeOrgId, "USER_LOGIN_SUCCESS", "USER",
                user.getId().toString(), "SUCCESS", UUID.randomUUID().toString(), "{}");

        UserDto userDto = new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.isEmailVerified(), user.getStatus());
        return AuthResponse.of(accessToken, rawRefreshToken, jwtService.getAccessTokenExpirationSeconds(), userDto, activeOrg, organizations);
    }

    @Transactional
    public void forgotPassword(String email) {
        String normalizedEmail = SecurityUtils.normalizeEmail(email);
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUserId(user.getId());

            String rawToken = SecurityUtils.generateSecureToken(32);
            String tokenHash = SecurityUtils.sha256Hex(rawToken);
            Instant expiresAt = Instant.now().plusSeconds(15 * 60); // 15 minutes

            PasswordResetToken resetToken = new PasswordResetToken(user.getId(), tokenHash, expiresAt);
            passwordResetTokenRepository.save(resetToken);

            emailService.sendPasswordResetEmail(normalizedEmail, rawToken);

            auditService.logEvent(user.getId(), null, "PASSWORD_RESET_REQUEST", "USER",
                    user.getId().toString(), "SUCCESS", UUID.randomUUID().toString(), "{}");
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = SecurityUtils.sha256Hex(request.token());
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired password reset token"));

        if (!token.isValid()) {
            throw new ConflictException("Password reset token has already been used or expired");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.resetFailedLogin();
        userRepository.save(user);

        token.markUsed();
        passwordResetTokenRepository.save(token);

        // Invalidate all active sessions and refresh tokens on password reset!
        refreshTokenService.revokeAllForUser(user.getId());

        auditService.logEvent(user.getId(), null, "PASSWORD_RESET_SUCCESS", "USER",
                user.getId().toString(), "SUCCESS", UUID.randomUUID().toString(), "{}");
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    @Transactional
    public UserDto updateProfile(UUID userId, String fullName) {
        User user = getUserById(userId);
        user.setFullName(fullName);
        user = userRepository.save(user);

        auditService.logEvent(user.getId(), null, "USER_PROFILE_UPDATE", "USER",
                user.getId().toString(), "SUCCESS", UUID.randomUUID().toString(),
                "{\"fullName\":\"" + fullName + "\"}");

        return new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.isEmailVerified(), user.getStatus());
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);

        if (user.getPasswordHash() == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            auditService.logEvent(user.getId(), null, "PASSWORD_CHANGE_FAILED", "USER",
                    user.getId().toString(), "FAILURE", UUID.randomUUID().toString(), "{}");
            throw new UnauthorizedException("Current password does not match");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidate active sessions to prevent hijacked sessions continuing after password change
        refreshTokenService.revokeAllForUser(user.getId());

        auditService.logEvent(user.getId(), null, "PASSWORD_CHANGE_SUCCESS", "USER",
                user.getId().toString(), "SUCCESS", UUID.randomUUID().toString(), "{}");
    }
}
