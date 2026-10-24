package com.securegate.authentication.web;

import com.securegate.authentication.service.MagicLinkService;
import com.securegate.identity.dto.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

import static com.securegate.identity.web.AuthController.REFRESH_COOKIE_NAME;

@RestController
@RequestMapping("/api/v1/auth/magic-link")
@Tag(name = "Magic Links", description = "Passwordless email magic-link issuance and authentication")
public class MagicLinkController {

    private final MagicLinkService magicLinkService;

    public record SendMagicLinkRequest(
            @NotBlank @Email String email
    ) {}

    public record VerifyMagicLinkRequest(
            @NotBlank String token
    ) {}

    public MagicLinkController(MagicLinkService magicLinkService) {
        this.magicLinkService = magicLinkService;
    }

    @PostMapping("/request")
    @Operation(summary = "Send single-use magic link email")
    public ResponseEntity<Map<String, String>> requestMagicLink(@Valid @RequestBody SendMagicLinkRequest request) {
        magicLinkService.sendMagicLink(request.email());
        return ResponseEntity.ok(Map.of("message", "If an account exists for this email, a sign-in link has been sent."));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify magic link token and issue authenticated session")
    public ResponseEntity<AuthResponse> verifyMagicLink(
            @Valid @RequestBody VerifyMagicLinkRequest request,
            @RequestParam(required = false) UUID tenantId,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();

        AuthResponse auth = magicLinkService.verifyMagicLink(request.token(), deviceInfo, ipAddress, tenantId);

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
}
