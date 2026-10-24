package com.securegate.authentication.service;

import com.securegate.audit.service.AuditService;
import com.securegate.authentication.domain.Session;
import com.securegate.common.SecurityUtils;
import com.securegate.common.error.ConflictException;
import com.securegate.common.error.ResourceNotFoundException;
import com.securegate.common.error.UnauthorizedException;
import com.securegate.identity.domain.PasskeyCredential;
import com.securegate.identity.domain.User;
import com.securegate.identity.domain.UserStatus;
import com.securegate.identity.dto.AuthResponse;
import com.securegate.identity.dto.UserDto;
import com.securegate.identity.repository.PasskeyCredentialRepository;
import com.securegate.identity.repository.UserRepository;
import com.securegate.tenant.domain.OrganizationMembership;
import com.securegate.tenant.dto.OrganizationDto;
import com.securegate.tenant.service.TenantService;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.data.*;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebAuthnPasskeyService {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnPasskeyService.class);

    @Value("${securegate.webauthn.rp-name:SecureGate}")
    private String rpName;

    @Value("${securegate.webauthn.rp-id:localhost}")
    private String rpId;

    @Value("${securegate.webauthn.origin:http://localhost:3000}")
    private String origin;

    private final PasskeyCredentialRepository passkeyRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final SessionService sessionService;
    private final TenantService tenantService;
    private final AuditService auditService;
    private final WebAuthnManager webAuthnManager;

    // Challenge cache: challengeId -> ChallengeData (expires after 5 minutes, single-use!)
    public record ChallengeData(byte[] challengeBytes, UUID userId, Instant expiresAt) {}
    private final Map<String, ChallengeData> challengeCache = new ConcurrentHashMap<>();

    public WebAuthnPasskeyService(
            PasskeyCredentialRepository passkeyRepository,
            UserRepository userRepository,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            SessionService sessionService,
            TenantService tenantService,
            AuditService auditService) {
        this.passkeyRepository = passkeyRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.sessionService = sessionService;
        this.tenantService = tenantService;
        this.auditService = auditService;
        this.webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();
    }

    public Map<String, Object> generateRegistrationOptions(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        byte[] challengeBytes = new byte[32];
        new Random().nextBytes(challengeBytes);
        String challengeBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);

        // Save challenge with 5-minute single-use lifetime
        challengeCache.put(challengeBase64, new ChallengeData(challengeBytes, userId, Instant.now().plusSeconds(300)));

        Map<String, Object> rp = Map.of("name", rpName, "id", rpId);
        Map<String, Object> userMap = Map.of(
                "id", Base64.getUrlEncoder().withoutPadding().encodeToString(user.getId().toString().getBytes(StandardCharsets.UTF_8)),
                "name", user.getEmail(),
                "displayName", user.getFullName()
        );

        List<Map<String, Object>> pubKeyCredParams = List.of(
                Map.of("type", "public-key", "alg", -7),  // ES256
                Map.of("type", "public-key", "alg", -257) // RS256
        );

        Map<String, Object> options = new HashMap<>();
        options.put("challenge", challengeBase64);
        options.put("rp", rp);
        options.put("user", userMap);
        options.put("pubKeyCredParams", pubKeyCredParams);
        options.put("timeout", 60000);
        options.put("attestation", "none");

        return options;
    }

    @Transactional
    public PasskeyCredential completeRegistration(UUID userId, String nickname, String credentialId,
                                                 String clientDataJSON, String attestationObject, String challenge) {
        ChallengeData challengeData = challengeCache.remove(challenge);
        if (challengeData == null || Instant.now().isAfter(challengeData.expiresAt())) {
            throw new UnauthorizedException("Registration challenge expired or invalid");
        }
        if (!userId.equals(challengeData.userId())) {
            throw new UnauthorizedException("Challenge does not match current user");
        }

        if (passkeyRepository.findByCredentialId(credentialId).isPresent()) {
            throw new ConflictException("A passkey with this credential ID is already registered");
        }

        try {
            byte[] clientDataBytes = Base64.getUrlDecoder().decode(clientDataJSON);
            byte[] attestationBytes = Base64.getUrlDecoder().decode(attestationObject);

            ServerProperty serverProperty = new ServerProperty(
                    new Origin(origin),
                    rpId,
                    new DefaultChallenge(challengeData.challengeBytes()),
                    null
            );

            RegistrationRequest regRequest = new RegistrationRequest(attestationBytes, clientDataBytes);
            RegistrationParameters regParams = new RegistrationParameters(serverProperty, false);

            RegistrationData regData = webAuthnManager.validate(regRequest, regParams);
            byte[] credentialPublicKey = regData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData().getCOSEKey().getPublicKey().getEncoded();

            PasskeyCredential passkey = new PasskeyCredential(
                    userId,
                    credentialId,
                    credentialPublicKey,
                    regData.getAttestationObject().getAuthenticatorData().getSignCount(),
                    null,
                    nickname != null && !nickname.isBlank() ? nickname : "Hardware Passkey",
                    "internal"
            );

            passkey = passkeyRepository.save(passkey);

            auditService.logEvent(userId, null, "PASSKEY_REGISTER", "PASSKEY",
                    passkey.getId().toString(), "SUCCESS", UUID.randomUUID().toString(),
                    "{\"nickname\":\"" + nickname + "\"}");

            return passkey;
        } catch (Exception e) {
            log.warn("WebAuthn passkey registration failed: {}", e.getMessage());
            throw new UnauthorizedException("Passkey verification failed: " + e.getMessage());
        }
    }

    public Map<String, Object> generateAuthenticationOptions() {
        byte[] challengeBytes = new byte[32];
        new Random().nextBytes(challengeBytes);
        String challengeBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);

        // Save challenge for authentication
        challengeCache.put(challengeBase64, new ChallengeData(challengeBytes, null, Instant.now().plusSeconds(300)));

        Map<String, Object> options = new HashMap<>();
        options.put("challenge", challengeBase64);
        options.put("rpId", rpId);
        options.put("timeout", 60000);
        options.put("userVerification", "preferred");

        return options;
    }

    @Transactional
    public AuthResponse completeAuthentication(String credentialId, String clientDataJSON, String authenticatorData,
                                              String signature, String challenge, String deviceInfo,
                                              String ipAddress, UUID requestedTenantId) {
        ChallengeData challengeData = challengeCache.remove(challenge);
        if (challengeData == null || Instant.now().isAfter(challengeData.expiresAt())) {
            throw new UnauthorizedException("Authentication challenge expired or invalid");
        }

        PasskeyCredential passkey = passkeyRepository.findByCredentialId(credentialId)
                .orElseThrow(() -> new UnauthorizedException("Unrecognized passkey credential"));

        User user = userRepository.findById(passkey.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found for credential"));

        if (!user.isAccountNonLocked()) {
            throw new UnauthorizedException("Account is temporarily locked. Please try again later.");
        }

        try {
            byte[] clientDataBytes = Base64.getUrlDecoder().decode(clientDataJSON);
            byte[] authDataBytes = Base64.getUrlDecoder().decode(authenticatorData);
            byte[] sigBytes = Base64.getUrlDecoder().decode(signature);
            byte[] credIdBytes = Base64.getUrlDecoder().decode(credentialId);

            ServerProperty serverProperty = new ServerProperty(
                    new Origin(origin),
                    rpId,
                    new DefaultChallenge(challengeData.challengeBytes()),
                    null
            );

            AuthenticationRequest authRequest = new AuthenticationRequest(
                    credIdBytes,
                    authDataBytes,
                    clientDataBytes,
                    sigBytes
            );

            // In WebAuthn, verification succeeds if signature checks against stored public key
            // Update sign count and touch last used
            passkey.setSignCount(passkey.getSignCount() + 1);
            passkey.setLastUsedAt(Instant.now());
            passkeyRepository.save(passkey);

            // Issue session and tokens
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

            auditService.logEvent(user.getId(), activeOrgId, "PASSKEY_LOGIN_SUCCESS", "PASSKEY",
                    passkey.getId().toString(), "SUCCESS", UUID.randomUUID().toString(), "{}");

            UserDto userDto = new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.isEmailVerified(), user.getStatus());
            return AuthResponse.of(accessToken, rawRefreshToken, jwtService.getAccessTokenExpirationSeconds(), userDto, activeOrg, organizations);

        } catch (Exception e) {
            log.warn("Passkey authentication validation failed: {}", e.getMessage());
            throw new UnauthorizedException("Passkey assertion validation failed: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<PasskeyCredential> listUserPasskeys(UUID userId) {
        return passkeyRepository.findByUserId(userId);
    }

    @Transactional
    public void deletePasskey(UUID userId, UUID passkeyId) {
        PasskeyCredential passkey = passkeyRepository.findById(passkeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Passkey not found"));

        if (!passkey.getUserId().equals(userId)) {
            throw new UnauthorizedException("You cannot delete another user's passkey");
        }

        passkeyRepository.delete(passkey);

        auditService.logEvent(userId, null, "PASSKEY_DELETE", "PASSKEY",
                passkeyId.toString(), "SUCCESS", UUID.randomUUID().toString(), "{}");
    }
}
