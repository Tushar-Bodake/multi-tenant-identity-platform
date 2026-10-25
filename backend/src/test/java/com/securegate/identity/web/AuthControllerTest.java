package com.securegate.identity.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securegate.authentication.service.JwtService;
import com.securegate.authentication.service.RefreshTokenService;
import com.securegate.common.error.ConflictException;
import com.securegate.common.error.UnauthorizedException;
import com.securegate.common.security.JwtAuthenticationFilter;
import com.securegate.identity.domain.User;
import com.securegate.identity.domain.UserStatus;
import com.securegate.identity.dto.*;
import com.securegate.identity.service.IdentityService;
import com.securegate.tenant.dto.OrganizationDto;
import com.securegate.tenant.service.TenantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for AuthController — mocks all service dependencies and
 * exercises HTTP-layer validation, response shapes, and error handling
 * without starting a full Spring context.
 */
@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private IdentityService identityService;
    @MockBean private RefreshTokenService refreshTokenService;
    @MockBean private TenantService tenantService;
    @MockBean private JwtService jwtService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORG_ID  = UUID.randomUUID();

    // -----------------------------------------------------------------------
    // POST /api/v1/auth/register
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /register — 201 Created with valid payload")
    @WithAnonymousUser
    void testRegisterSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest("Alice Test", "alice@example.com", "SecurePassword123!", "Acme Corp");

        UserDto dto = new UserDto(USER_ID, "Alice Test", "alice@example.com", false, UserStatus.PENDING_VERIFICATION);
        when(identityService.register(any(RegisterRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"));
    }

    @Test
    @DisplayName("POST /register — 400 Bad Request for missing required fields")
    @WithAnonymousUser
    void testRegisterBadRequest() throws Exception {
        // Missing password and organizationName
        String badJson = "{\"fullName\":\"Alice\",\"email\":\"alice@example.com\"}";

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(identityService);
    }

    @Test
    @DisplayName("POST /register — 409 Conflict when email already taken")
    @WithAnonymousUser
    void testRegisterConflict() throws Exception {
        RegisterRequest req = new RegisterRequest("Alice Test", "alice@example.com", "SecurePassword123!", "Acme Corp");
        when(identityService.register(any())).thenThrow(new ConflictException("An account with this email already exists"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // -----------------------------------------------------------------------
    // POST /api/v1/auth/login
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /login — 200 OK with valid credentials and refresh cookie set")
    @WithAnonymousUser
    void testLoginSuccess() throws Exception {
        LoginRequest req = new LoginRequest("alice@example.com", "SecurePassword123!");

        UserDto userDto = new UserDto(USER_ID, "Alice Test", "alice@example.com", true, UserStatus.ACTIVE);
        OrganizationDto orgDto = new OrganizationDto(ORG_ID, "Acme Corp", "acme-corp", "ACTIVE", "ORG_OWNER");
        AuthResponse authResponse = AuthResponse.of("access.jwt.token", "raw-refresh-token",
                900, userDto, orgDto, List.of(orgDto));

        when(identityService.login(any(LoginRequest.class), any(), any(), any())).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.jwt.token"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.email").value("alice@example.com"))
                .andExpect(cookie().exists(AuthController.REFRESH_COOKIE_NAME))
                .andExpect(cookie().httpOnly(AuthController.REFRESH_COOKIE_NAME, true));
    }

    @Test
    @DisplayName("POST /login — 401 Unauthorized for wrong credentials")
    @WithAnonymousUser
    void testLoginInvalidCredentials() throws Exception {
        LoginRequest req = new LoginRequest("alice@example.com", "WrongPassword!");
        when(identityService.login(any(), any(), any(), any()))
                .thenThrow(new UnauthorizedException("Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // POST /api/v1/auth/verify-email
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /verify-email — 200 OK with valid token")
    @WithAnonymousUser
    void testVerifyEmailSuccess() throws Exception {
        VerifyEmailRequest req = new VerifyEmailRequest("valid-raw-token");

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("verified")));

        verify(identityService).verifyEmail("valid-raw-token");
    }

    // -----------------------------------------------------------------------
    // POST /api/v1/auth/forgot-password
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /forgot-password — 200 OK (ambiguous to prevent email enumeration)")
    @WithAnonymousUser
    void testForgotPasswordAlwaysOk() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest("anyone@example.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("If that email is registered")));

        verify(identityService).forgotPassword("anyone@example.com");
    }

    // -----------------------------------------------------------------------
    // POST /api/v1/auth/logout
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /logout — 200 OK and refresh token is revoked")
    @WithAnonymousUser
    void testLogout() throws Exception {
        AuthController.RefreshTokenRequest req = new AuthController.RefreshTokenRequest("raw-refresh-token");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("Logged out")));

        verify(refreshTokenService).revokeToken("raw-refresh-token");
    }
}
