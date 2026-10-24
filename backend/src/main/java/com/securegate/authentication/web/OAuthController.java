package com.securegate.authentication.web;

import com.securegate.authentication.service.OAuthService;
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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.securegate.identity.web.AuthController.REFRESH_COOKIE_NAME;

@RestController
@RequestMapping("/api/v1/auth/oauth")
@Tag(name = "OAuth 2.0 / OIDC", description = "Federated single sign-on with Google and GitHub")
public class OAuthController {

    private final OAuthService oAuthService;

    public record OAuthCallbackRequest(
            @NotBlank String provider,
            @NotBlank String providerSubject,
            @NotBlank String email,
            String name,
            boolean confirmLink
    ) {}

    public OAuthController(OAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    @PostMapping("/callback")
    @Operation(summary = "Process OAuth callback and authenticate or link user identity")
    public ResponseEntity<AuthResponse> handleOAuthCallback(
            @Valid @RequestBody OAuthCallbackRequest request,
            @RequestParam(required = false) UUID tenantId,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();

        OAuthService.OAuthUserProfile profile = new OAuthService.OAuthUserProfile(
                request.provider(),
                request.providerSubject(),
                request.email(),
                request.name()
        );

        AuthResponse auth = oAuthService.handleOAuthLogin(
                profile,
                request.confirmLink(),
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
}
