package com.securegate.identity.web;

import com.securegate.authentication.domain.Session;
import com.securegate.authentication.service.SessionService;
import com.securegate.identity.domain.User;
import com.securegate.identity.dto.UserDto;
import com.securegate.identity.service.IdentityService;
import com.securegate.tenant.dto.OrganizationDto;
import com.securegate.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "User Profile & Sessions", description = "Endpoints for managing user profile, password, and active sessions")
public class UserController {

    private final IdentityService identityService;
    private final TenantService tenantService;
    private final SessionService sessionService;

    public record UpdateProfileRequest(
            @NotBlank @Size(min = 2, max = 255) String fullName
    ) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 128) String newPassword
    ) {}

    public UserController(
            IdentityService identityService,
            TenantService tenantService,
            SessionService sessionService) {
        this.identityService = identityService;
        this.tenantService = tenantService;
        this.sessionService = sessionService;
    }

    @GetMapping
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<Map<String, Object>> getProfile(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        User user = identityService.getUserById(userId);
        List<OrganizationDto> organizations = tenantService.getUserOrganizations(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("user", new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.isEmailVerified(), user.getStatus()));
        response.put("organizations", organizations);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    @Operation(summary = "Update current user profile information")
    public ResponseEntity<UserDto> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        UserDto updated = identityService.updateProfile(userId, request.fullName());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password for authenticated account")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        identityService.changePassword(userId, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully. All other sessions have been invalidated."));
    }

    @GetMapping("/sessions")
    @Operation(summary = "List all active sessions for current user")
    public ResponseEntity<List<Session>> getActiveSessions(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        return ResponseEntity.ok(sessionService.getUserActiveSessions(userId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Revoke an active session by ID")
    public ResponseEntity<Map<String, String>> revokeSession(
            @PathVariable UUID sessionId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        // Verify session belongs to user
        sessionService.getSession(sessionId).ifPresent(session -> {
            if (session.getUserId().equals(userId)) {
                sessionService.revokeSession(sessionId);
            }
        });
        return ResponseEntity.ok(Map.of("message", "Session revoked successfully"));
    }

    @PostMapping("/sessions/revoke-all")
    @Operation(summary = "Revoke all active sessions for current user")
    public ResponseEntity<Map<String, String>> revokeAllSessions(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        sessionService.revokeAllUserSessions(userId);
        return ResponseEntity.ok(Map.of("message", "All active sessions revoked"));
    }
}
