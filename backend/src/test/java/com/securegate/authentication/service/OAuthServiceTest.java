package com.securegate.authentication.service;

import com.securegate.audit.service.AuditService;
import com.securegate.authentication.domain.Session;
import com.securegate.common.error.ConflictException;
import com.securegate.identity.domain.ExternalIdentity;
import com.securegate.identity.domain.User;
import com.securegate.identity.domain.UserEmail;
import com.securegate.identity.domain.UserStatus;
import com.securegate.identity.dto.AuthResponse;
import com.securegate.identity.repository.ExternalIdentityRepository;
import com.securegate.identity.repository.UserEmailRepository;
import com.securegate.identity.repository.UserRepository;
import com.securegate.tenant.domain.Organization;
import com.securegate.tenant.domain.OrganizationMembership;
import com.securegate.tenant.domain.Role;
import com.securegate.tenant.dto.OrganizationDto;
import com.securegate.tenant.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserEmailRepository userEmailRepository;

    @Mock
    private ExternalIdentityRepository externalIdentityRepository;

    @Mock
    private TenantService tenantService;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private SessionService sessionService;

    @Mock
    private AuditService auditService;

    private OAuthService oAuthService;

    @BeforeEach
    void setUp() {
        oAuthService = new OAuthService(
                userRepository,
                userEmailRepository,
                externalIdentityRepository,
                tenantService,
                jwtService,
                refreshTokenService,
                sessionService,
                auditService
        );
    }

    @Test
    @DisplayName("Should login directly when external identity is already linked")
    void testExistingLinkedIdentityLogin() {
        UUID userId = UUID.randomUUID();
        User user = new User("Alice Developer", "alice@github.com", null);
        user.setId(userId);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);

        ExternalIdentity linked = new ExternalIdentity(userId, "GITHUB", "gh_12345", "alice@github.com");

        when(externalIdentityRepository.findByProviderAndProviderSubject("GITHUB", "gh_12345"))
                .thenReturn(Optional.of(linked));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        OrganizationDto orgDto = new OrganizationDto(UUID.randomUUID(), "Alice Org", "alice-org", "ACTIVE", "ORG_OWNER");
        when(tenantService.getUserOrganizations(userId)).thenReturn(List.of(orgDto));

        Role role = new Role(UUID.randomUUID(), "ORG_OWNER", "Owner", true);
        OrganizationMembership membership = new OrganizationMembership(orgDto.id(), userId, role);
        when(tenantService.getMembership(orgDto.id(), userId)).thenReturn(membership);

        Session session = new Session(userId, "device", "127.0.0.1", java.time.Instant.now().plusSeconds(3600));
        when(sessionService.createSession(eq(userId), any(), any(), anyLong())).thenReturn(session);
        when(refreshTokenService.issueInitialRefreshToken(userId, session.getId())).thenReturn("raw_refresh_token");
        when(jwtService.generateAccessToken(eq(userId), eq("alice@github.com"), eq("Alice Developer"), eq(orgDto.id()), eq("ORG_OWNER"), any()))
                .thenReturn("access_token_jwt");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        OAuthService.OAuthUserProfile profile = new OAuthService.OAuthUserProfile("GITHUB", "gh_12345", "alice@github.com", "Alice Developer");
        AuthResponse response = oAuthService.handleOAuthLogin(profile, false, "device", "127.0.0.1", null);

        assertNotNull(response);
        assertEquals("access_token_jwt", response.accessToken());
        assertEquals("raw_refresh_token", response.refreshToken());
        assertEquals("Alice Developer", response.user().fullName());
    }

    @Test
    @DisplayName("Security Requirement: Linking unconfirmed external identity to existing password account must throw ConflictException")
    void testUnconfirmedAccountLinkingThrowsConflict() {
        UUID existingUserId = UUID.randomUUID();
        User existingUser = new User("Bob Manager", "bob@example.com", "$argon2id$...password");
        existingUser.setId(existingUserId);

        when(externalIdentityRepository.findByProviderAndProviderSubject("GOOGLE", "goog_999"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(existingUser));

        OAuthService.OAuthUserProfile profile = new OAuthService.OAuthUserProfile("GOOGLE", "goog_999", "bob@example.com", "Bob Google");

        // confirmLink is false -> must throw ConflictException to prevent silent hijacking
        assertThrows(ConflictException.class, () ->
                oAuthService.handleOAuthLogin(profile, false, "device", "127.0.0.1", null)
        );

        verify(externalIdentityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should link external identity when confirmLink is explicitly set to true")
    void testConfirmedAccountLinkingSuccess() {
        UUID existingUserId = UUID.randomUUID();
        User existingUser = new User("Bob Manager", "bob@example.com", "$argon2id$...password");
        existingUser.setId(existingUserId);
        existingUser.setStatus(UserStatus.ACTIVE);
        existingUser.setEmailVerified(true);

        when(externalIdentityRepository.findByProviderAndProviderSubject("GOOGLE", "goog_999"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(existingUser));

        OrganizationDto orgDto = new OrganizationDto(UUID.randomUUID(), "Bob Corp", "bob-corp", "ACTIVE", "ORG_OWNER");
        when(tenantService.getUserOrganizations(existingUserId)).thenReturn(List.of(orgDto));

        Role role = new Role(UUID.randomUUID(), "ORG_OWNER", "Owner", true);
        OrganizationMembership membership = new OrganizationMembership(orgDto.id(), existingUserId, role);
        when(tenantService.getMembership(orgDto.id(), existingUserId)).thenReturn(membership);

        Session session = new Session(existingUserId, "device", "127.0.0.1", java.time.Instant.now().plusSeconds(3600));
        when(sessionService.createSession(eq(existingUserId), any(), any(), anyLong())).thenReturn(session);
        when(refreshTokenService.issueInitialRefreshToken(existingUserId, session.getId())).thenReturn("raw_refresh_token");
        when(jwtService.generateAccessToken(any(), any(), any(), any(), any(), any())).thenReturn("access_token_jwt");

        OAuthService.OAuthUserProfile profile = new OAuthService.OAuthUserProfile("GOOGLE", "goog_999", "bob@example.com", "Bob Google");

        AuthResponse response = oAuthService.handleOAuthLogin(profile, true, "device", "127.0.0.1", null);

        assertNotNull(response);
        verify(externalIdentityRepository, times(1)).save(any(ExternalIdentity.class));
        verify(auditService, times(1)).logEvent(eq(existingUserId), any(), eq("OAUTH_IDENTITY_LINKED"), eq("EXTERNAL_IDENTITY"), any(), eq("SUCCESS"), any(), any());
    }

    @Test
    @DisplayName("Should auto-provision new user and default organization on first OAuth sign-in")
    void testAutoProvisionNewUser() {
        when(externalIdentityRepository.findByProviderAndProviderSubject("GITHUB", "new_gh_123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("newuser@github.com")).thenReturn(Optional.empty());

        UUID newUserId = UUID.randomUUID();
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(newUserId);
            return u;
        });

        Organization org = new Organization("New User's Team", "new-users-team");
        when(tenantService.createOrganization(anyString(), any(User.class))).thenReturn(org);

        OrganizationDto orgDto = new OrganizationDto(org.getId(), org.getName(), org.getSlug(), "ACTIVE", "ORG_OWNER");
        when(tenantService.getUserOrganizations(newUserId)).thenReturn(List.of(orgDto));

        Role role = new Role(UUID.randomUUID(), "ORG_OWNER", "Owner", true);
        OrganizationMembership membership = new OrganizationMembership(org.getId(), newUserId, role);
        when(tenantService.getMembership(org.getId(), newUserId)).thenReturn(membership);

        Session session = new Session(newUserId, "device", "127.0.0.1", java.time.Instant.now().plusSeconds(3600));
        when(sessionService.createSession(any(), any(), any(), anyLong())).thenReturn(session);
        when(refreshTokenService.issueInitialRefreshToken(any(), any())).thenReturn("raw_refresh");
        when(jwtService.generateAccessToken(any(), any(), any(), any(), any(), any())).thenReturn("jwt_token");

        OAuthService.OAuthUserProfile profile = new OAuthService.OAuthUserProfile("GITHUB", "new_gh_123", "newuser@github.com", "New User");
        AuthResponse response = oAuthService.handleOAuthLogin(profile, false, "device", "127.0.0.1", null);

        assertNotNull(response);
        verify(userRepository, times(1)).save(any(User.class));
        verify(userEmailRepository, times(1)).save(any(UserEmail.class));
        verify(externalIdentityRepository, times(1)).save(any(ExternalIdentity.class));
        verify(tenantService, times(1)).createOrganization(eq("New User's Team"), any(User.class));
    }
}
