package com.securegate.authentication.web;

import com.securegate.authentication.service.WebAuthnPasskeyService;
import com.securegate.identity.domain.PasskeyCredential;
import com.securegate.identity.dto.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.securegate.identity.web.AuthController.REFRESH_COOKIE_NAME;

@RestController
@RequestMapping("/api/v1/auth/passkeys")
@Tag(name = "Passkeys (WebAuthn)", description = "FIDO2 / WebAuthn passkey registration and passwordless login")
public class PasskeyController {

    private final WebAuthnPasskeyService passkeyService;

    public record VerifyRegistrationRequest(
            String nickname,
            @NotBlank String credentialId,
            @NotBlank String clientDataJSON,
            @NotBlank String attestationObject,
            @NotBlank String challenge
    ) {}

    public record VerifyAuthenticationRequest(
            @NotBlank String credentialId,
            @NotBlank String clientDataJSON,
            @NotBlank String authenticatorData,
            @NotBlank String signature,
            @NotBlank String challenge
    ) {}

    public PasskeyController(WebAuthnPasskeyService passkeyService) {
        this.passkeyService = passkeyService;
    }

    @GetMapping("/register/options")
    @Operation(summary = "Generate WebAuthn registration options and challenge")
    public ResponseEntity<Map<String, Object>> getRegistrationOptions(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        return ResponseEntity.ok(passkeyService.generateRegistrationOptions(userId));
    }

    @PostMapping("/register/verify")
    @Operation(summary = "Complete WebAuthn registration and enroll passkey")
    public ResponseEntity<PasskeyCredential> verifyRegistration(
            @Valid @RequestBody VerifyRegistrationRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        PasskeyCredential passkey = passkeyService.completeRegistration(
                userId,
                request.nickname(),
                request.credentialId(),
                request.clientDataJSON(),
                request.attestationObject(),
                request.challenge()
        );
        return ResponseEntity.ok(passkey);
    }

    @GetMapping("/login/options")
    @Operation(summary = "Generate WebAuthn authentication challenge for passwordless sign-in")
    public ResponseEntity<Map<String, Object>> getAuthenticationOptions() {
        return ResponseEntity.ok(passkeyService.generateAuthenticationOptions());
    }

    @PostMapping("/login/verify")
    @Operation(summary = "Verify passkey assertion and issue authenticated tokens")
    public ResponseEntity<AuthResponse> verifyAuthentication(
            @Valid @RequestBody VerifyAuthenticationRequest request,
            @RequestParam(required = false) UUID tenantId,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();

        AuthResponse auth = passkeyService.completeAuthentication(
                request.credentialId(),
                request.clientDataJSON(),
                request.authenticatorData(),
                request.signature(),
                request.challenge(),
                deviceInfo,
                ipAddress,
                tenantId
        );

        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, auth.refreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth")
                .maxAge(7 * 24 * 3600)
                .sameSite("Lax")
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(auth);
    }

    @GetMapping
    @Operation(summary = "List all passkeys enrolled by the current user")
    public ResponseEntity<List<PasskeyCredential>> listPasskeys(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        return ResponseEntity.ok(passkeyService.listUserPasskeys(userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Revoke an enrolled passkey")
    public ResponseEntity<Map<String, String>> deletePasskey(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        passkeyService.deletePasskey(userId, id);
        return ResponseEntity.ok(Map.of("message", "Passkey deleted successfully"));
    }
}
