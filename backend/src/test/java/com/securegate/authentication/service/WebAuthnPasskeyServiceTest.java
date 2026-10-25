package com.securegate.authentication.service;

import com.securegate.audit.service.AuditService;
import com.securegate.common.error.ResourceNotFoundException;
import com.securegate.common.error.UnauthorizedException;
import com.securegate.identity.domain.PasskeyCredential;
import com.securegate.identity.domain.User;
import com.securegate.identity.domain.UserStatus;
import com.securegate.identity.repository.PasskeyCredentialRepository;
import com.securegate.identity.repository.UserRepository;
import com.securegate.tenant.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebAuthnPasskeyServiceTest {

    @Mock
    private PasskeyCredentialRepository passkeyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private SessionService sessionService;

    @Mock
    private TenantService tenantService;

    @Mock
    private AuditService auditService;

    private WebAuthnPasskeyService passkeyService;

    @BeforeEach
    void setUp() {
        passkeyService = new WebAuthnPasskeyService(
                passkeyRepository,
                userRepository,
                jwtService,
                refreshTokenService,
                sessionService,
                tenantService,
                auditService
        );
    }

    @Test
    @DisplayName("Should generate valid WebAuthn registration options with single-use challenge")
    void testGenerateRegistrationOptions() {
        UUID userId = UUID.randomUUID();
        User user = new User("Alice Developer", "alice@securegate.local", "hashedPassword");
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Map<String, Object> options = passkeyService.generateRegistrationOptions(userId);

        assertNotNull(options);
        assertNotNull(options.get("challenge"));
        assertNotNull(options.get("rp"));
        assertNotNull(options.get("user"));
        assertNotNull(options.get("pubKeyCredParams"));
        assertEquals("none", options.get("attestation"));
    }

    @Test
    @DisplayName("Should generate WebAuthn authentication challenge options")
    void testGenerateAuthenticationOptions() {
        Map<String, Object> options = passkeyService.generateAuthenticationOptions();

        assertNotNull(options);
        assertNotNull(options.get("challenge"));
        assertEquals("preferred", options.get("userVerification"));
    }

    @Test
    @DisplayName("Negative Test: Registration with non-existent or replayed challenge must throw UnauthorizedException")
    void testRegistrationReplayedChallengeThrows() {
        UUID userId = UUID.randomUUID();
        String fakeChallenge = "unrecognized_or_replayed_challenge_token";

        assertThrows(UnauthorizedException.class, () ->
                passkeyService.completeRegistration(
                        userId,
                        "Hardware Key",
                        "cred_123",
                        "clientData",
                        "attestation",
                        fakeChallenge
                )
        );
    }

    @Test
    @DisplayName("Negative Test: Authentication with invalid or expired challenge must throw UnauthorizedException")
    void testAuthenticationInvalidChallengeThrows() {
        assertThrows(UnauthorizedException.class, () ->
                passkeyService.completeAuthentication(
                        "cred_123",
                        "clientData",
                        "authData",
                        "sig",
                        "invalid_challenge",
                        "Chrome/Linux",
                        "127.0.0.1",
                        null
                )
        );
    }

    @Test
    @DisplayName("Negative Test: Deleting another user's passkey must throw UnauthorizedException")
    void testDeleteOtherUserPasskeyThrows() {
        UUID ownerId = UUID.randomUUID();
        UUID attackerId = UUID.randomUUID();
        PasskeyCredential credential = new PasskeyCredential(
                ownerId,
                "cred_id_abc",
                new byte[]{1, 2, 3},
                0L,
                null,
                "YubiKey",
                "usb"
        );
        UUID passkeyId = credential.getId();

        when(passkeyRepository.findById(passkeyId)).thenReturn(Optional.of(credential));

        assertThrows(UnauthorizedException.class, () ->
                passkeyService.deletePasskey(attackerId, passkeyId)
        );

        verify(passkeyRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should successfully delete passkey belonging to authenticated user")
    void testDeleteOwnPasskeySuccess() {
        UUID ownerId = UUID.randomUUID();

        PasskeyCredential credential = new PasskeyCredential(
                ownerId,
                "cred_id_abc",
                new byte[]{1, 2, 3},
                0L,
                null,
                "YubiKey",
                "usb"
        );
        UUID passkeyId = credential.getId();

        when(passkeyRepository.findById(passkeyId)).thenReturn(Optional.of(credential));

        passkeyService.deletePasskey(ownerId, passkeyId);

        verify(passkeyRepository, times(1)).delete(credential);
        verify(auditService, times(1)).logEvent(eq(ownerId), any(), eq("PASSKEY_DELETE"), eq("PASSKEY"), eq(passkeyId.toString()), eq("SUCCESS"), any(), any());
    }

    @Test
    @DisplayName("Should list user passkeys from repository")
    void testListUserPasskeys() {
        UUID userId = UUID.randomUUID();
        PasskeyCredential credential = new PasskeyCredential(userId, "cred_id", new byte[]{1}, 0L, null, "TouchID", "internal");

        when(passkeyRepository.findByUserId(userId)).thenReturn(List.of(credential));

        List<PasskeyCredential> result = passkeyService.listUserPasskeys(userId);
        assertEquals(1, result.size());
        assertEquals("TouchID", result.get(0).getNickname());
    }
}
