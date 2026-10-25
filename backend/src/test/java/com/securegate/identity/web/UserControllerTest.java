package com.securegate.identity.web;

import com.securegate.authentication.domain.Session;
import com.securegate.authentication.service.SessionService;
import com.securegate.identity.domain.User;
import com.securegate.identity.dto.UserDto;
import com.securegate.identity.service.IdentityService;
import com.securegate.tenant.dto.OrganizationDto;
import com.securegate.tenant.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private IdentityService identityService;
    @Mock private TenantService tenantService;
    @Mock private SessionService sessionService;
    @Mock private Authentication authentication;

    private UserController userController;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userController = new UserController(identityService, tenantService, sessionService);
        lenient().when(authentication.getPrincipal()).thenReturn(userId.toString());
    }

    @Test
    @DisplayName("Should return user profile and organizations")
    void testGetProfile() {
        User user = new User("Alice", "alice@example.com", "hash");
        user.setId(userId);
        List<OrganizationDto> orgs = List.of(new OrganizationDto(UUID.randomUUID(), "Acme Corp", "acme", "ACTIVE", "ORG_OWNER"));

        when(identityService.getUserById(userId)).thenReturn(user);
        when(tenantService.getUserOrganizations(userId)).thenReturn(orgs);

        ResponseEntity<Map<String, Object>> response = userController.getProfile(authentication);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsKeys("user", "organizations");
    }

    @Test
    @DisplayName("Should update user profile")
    void testUpdateProfile() {
        UserController.UpdateProfileRequest request = new UserController.UpdateProfileRequest("Alice Updated");
        UserDto updatedDto = new UserDto(userId, "Alice Updated", "alice@example.com", true, com.securegate.identity.domain.UserStatus.ACTIVE);

        when(identityService.updateProfile(userId, "Alice Updated")).thenReturn(updatedDto);

        ResponseEntity<UserDto> response = userController.updateProfile(request, authentication);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().fullName()).isEqualTo("Alice Updated");
    }

    @Test
    @DisplayName("Should change user password")
    void testChangePassword() {
        UserController.ChangePasswordRequest request = new UserController.ChangePasswordRequest("OldPassword123!", "NewPassword123!");

        ResponseEntity<Map<String, String>> response = userController.changePassword(request, authentication);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(identityService).changePassword(userId, "OldPassword123!", "NewPassword123!");
    }

    @Test
    @DisplayName("Should list and revoke active sessions")
    void testSessionsManagement() {
        UUID sessionId = UUID.randomUUID();
        Session session = new Session(userId, "Chrome / Windows", "192.168.1.1", Instant.now().plusSeconds(3600));

        when(sessionService.getUserActiveSessions(userId)).thenReturn(List.of(session));
        when(sessionService.getSession(sessionId)).thenReturn(Optional.of(session));

        ResponseEntity<List<Session>> listResponse = userController.getActiveSessions(authentication);
        assertThat(listResponse.getBody()).hasSize(1);

        ResponseEntity<Map<String, String>> revokeResponse = userController.revokeSession(sessionId, authentication);
        assertThat(revokeResponse.getStatusCode().is2xxSuccessful()).isTrue();
        verify(sessionService).revokeSession(sessionId);
    }
}
