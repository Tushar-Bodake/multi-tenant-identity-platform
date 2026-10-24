package com.securegate.identity.web;

import com.securegate.authentication.service.JwtService;
import com.securegate.authentication.service.RefreshTokenService;
import com.securegate.common.error.UnauthorizedException;
import com.securegate.identity.domain.User;
import com.securegate.identity.dto.*;
import com.securegate.identity.service.IdentityService;
import com.securegate.tenant.domain.OrganizationMembership;
import com.securegate.tenant.dto.OrganizationDto;
import com.securegate.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for registration, login, token refresh, and lifecycle")
public class AuthController {

    public static final String REFRESH_COOKIE_NAME = "securegate_refresh_token";

    private final IdentityService identityService;
    private final RefreshTokenService refreshTokenService;
    private final TenantService tenantService;
    private final JwtService jwtService;

    public record MessageResponse(String message) {}
    public record RefreshTokenRequest(String refreshToken) {}

    public AuthController(
            IdentityService identityService,
            RefreshTokenService refreshTokenService,
            TenantService tenantService,
            JwtService jwtService) {
        this.identityService = identityService;
        this.refreshTokenService = refreshTokenService;
        this.tenantService = tenantService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user and organization")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        UserDto user = identityService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email and password")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestParam(required = false) UUID tenantId,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();

        AuthResponse auth = identityService.login(request, deviceInfo, ipAddress, tenantId);

        // Attach secure HttpOnly refresh token cookie
        setRefreshTokenCookie(httpResponse, auth.refreshToken());

        return ResponseEntity.ok(auth);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue new JWT access token")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String cookieRefreshToken,
            @RequestParam(required = false) UUID tenantId,
            HttpServletResponse httpResponse) {

        String rawToken = (cookieRefreshToken != null && !cookieRefreshToken.isBlank())
                ? cookieRefreshToken
                : (requestBody != null ? requestBody.refreshToken() : null);

        if (rawToken == null || rawToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        RefreshTokenService.RotatedTokenPair rotated = refreshTokenService.rotateRefreshToken(rawToken);
        setRefreshTokenCookie(httpResponse, rotated.newRawRefreshToken());

        User user = identityService.getUserById(rotated.userId());
        List<OrganizationDto> organizations = tenantService.getUserOrganizations(user.getId());

        OrganizationDto activeOrg = null;
        Set<String> permissions = new HashSet<>();

        if (!organizations.isEmpty()) {
            if (tenantId != null) {
                activeOrg = organizations.stream()
                        .filter(o -> o.id().equals(tenantId))
                        .findFirst()
                        .orElse(organizations.get(0));
            } else {
                activeOrg = organizations.get(0);
            }
            OrganizationMembership membership = tenantService.getMembership(activeOrg.id(), user.getId());
            membership.getRole().getPermissions().forEach(p -> permissions.add(p.getName()));
        }

        UUID activeOrgId = activeOrg != null ? activeOrg.id() : null;
        String activeRole = activeOrg != null ? activeOrg.role() : null;
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getFullName(),
                activeOrgId, activeRole, permissions);

        UserDto userDto = new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.isEmailVerified(), user.getStatus());
        return ResponseEntity.ok(AuthResponse.of(accessToken, rotated.newRawRefreshToken(),
                jwtService.getAccessTokenExpirationSeconds(), userDto, activeOrg, organizations));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke current refresh token session")
    public ResponseEntity<MessageResponse> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            HttpServletResponse response) {

        String rawToken = cookieRefreshToken != null ? cookieRefreshToken : (requestBody != null ? requestBody.refreshToken() : null);
        if (rawToken != null) {
            refreshTokenService.revokeToken(rawToken);
        }

        clearRefreshTokenCookie(response);
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify account email ownership")
    public ResponseEntity<MessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        identityService.verifyEmail(request.token());
        return ResponseEntity.ok(new MessageResponse("Email successfully verified"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset email")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        identityService.forgotPassword(request.email());
        return ResponseEntity.ok(new MessageResponse("If that email is registered, password reset instructions have been sent."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using valid reset token")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        identityService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Password has been reset successfully. Please log in with your new password."));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user and organization memberships")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UnauthorizedException("Not authenticated");
        }

        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        User user = identityService.getUserById(userId);
        List<OrganizationDto> organizations = tenantService.getUserOrganizations(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("user", new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.isEmailVerified(), user.getStatus()));
        response.put("organizations", organizations);
        response.put("authorities", authentication.getAuthorities());

        return ResponseEntity.ok(response);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(false) // Set to true in TLS/production
                .path("/api/v1/auth")
                .maxAge(7 * 24 * 3600)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .path("/api/v1/auth")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
