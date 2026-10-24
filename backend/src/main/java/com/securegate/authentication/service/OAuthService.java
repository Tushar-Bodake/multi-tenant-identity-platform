package com.securegate.authentication.service;

import com.securegate.audit.service.AuditService;
import com.securegate.authentication.domain.Session;
import com.securegate.common.SecurityUtils;
import com.securegate.common.error.ConflictException;
import com.securegate.common.error.UnauthorizedException;
import com.securegate.identity.domain.ExternalIdentity;
import com.securegate.identity.domain.User;
import com.securegate.identity.domain.UserEmail;
import com.securegate.identity.domain.UserStatus;
import com.securegate.identity.dto.AuthResponse;
import com.securegate.identity.dto.UserDto;
import com.securegate.identity.repository.ExternalIdentityRepository;
import com.securegate.identity.repository.UserEmailRepository;
import com.securegate.identity.repository.UserRepository;
import com.securegate.tenant.domain.Organization;
import com.securegate.tenant.domain.OrganizationMembership;
import com.securegate.tenant.dto.OrganizationDto;
import com.securegate.tenant.service.TenantService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class OAuthService {

    private final UserRepository userRepository;
    private final UserEmailRepository userEmailRepository;
    private final ExternalIdentityRepository externalIdentityRepository;
    private final TenantService tenantService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final SessionService sessionService;
    private final AuditService auditService;

    public record OAuthUserProfile(String provider, String providerSubject, String email, String name) {}

    public OAuthService(
            UserRepository userRepository,
            UserEmailRepository userEmailRepository,
            ExternalIdentityRepository externalIdentityRepository,
            TenantService tenantService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            SessionService sessionService,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.userEmailRepository = userEmailRepository;
        this.externalIdentityRepository = externalIdentityRepository;
        this.tenantService = tenantService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.sessionService = sessionService;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse handleOAuthLogin(OAuthUserProfile profile, boolean confirmLink, String deviceInfo, String ipAddress, UUID requestedTenantId) {
        String normalizedEmail = SecurityUtils.normalizeEmail(profile.email());

        // 1. Check if external identity is already linked
        Optional<ExternalIdentity> existingExternal = externalIdentityRepository
                .findByProviderAndProviderSubject(profile.provider().toUpperCase(), profile.providerSubject());

        User user;
        if (existingExternal.isPresent()) {
            user = userRepository.findById(existingExternal.get().getUserId())
                    .orElseThrow(() -> new UnauthorizedException("User account not found"));
        } else {
            // 2. Check if a user with this email already exists
            Optional<User> existingUser = userRepository.findByEmail(normalizedEmail);
            if (existingUser.isPresent()) {
                // Account linking safeguard: Require explicit confirmation when linking external identity to existing password account
                if (!confirmLink) {
                    throw new ConflictException("An account with email " + normalizedEmail + " already exists. Please confirm account linking to proceed.");
                }
                user = existingUser.get();
            } else {
                // Create new user from OAuth profile
                user = new User(profile.name() != null ? profile.name() : "OAuth User", normalizedEmail, null);
                user.setEmailVerified(true);
                user.setStatus(UserStatus.ACTIVE);
                user = userRepository.save(user);

                UserEmail userEmail = new UserEmail(user.getId(), normalizedEmail, true, true);
                userEmailRepository.save(userEmail);

                // Create initial organization
                String orgName = (profile.name() != null ? profile.name() : "My") + "'s Team";
                tenantService.createOrganization(orgName, user);
            }

            // Save external identity link
            ExternalIdentity link = new ExternalIdentity(user.getId(), profile.provider().toUpperCase(), profile.providerSubject(), normalizedEmail);
            externalIdentityRepository.save(link);

            auditService.logEvent(user.getId(), null, "OAUTH_IDENTITY_LINKED", "EXTERNAL_IDENTITY",
                    profile.provider().toUpperCase() + ":" + profile.providerSubject(), "SUCCESS", UUID.randomUUID().toString(), "{}");
        }

        // Issue session & tokens
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

        auditService.logEvent(user.getId(), activeOrgId, "OAUTH_LOGIN_SUCCESS", "USER",
                user.getId().toString(), "SUCCESS", UUID.randomUUID().toString(),
                "{\"provider\":\"" + profile.provider() + "\"}");

        UserDto userDto = new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.isEmailVerified(), user.getStatus());
        return AuthResponse.of(accessToken, rawRefreshToken, jwtService.getAccessTokenExpirationSeconds(), userDto, activeOrg, organizations);
    }
}
