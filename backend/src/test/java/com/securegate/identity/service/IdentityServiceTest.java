package com.securegate.identity.service;

import com.securegate.audit.service.AuditService;
import com.securegate.authentication.service.JwtService;
import com.securegate.authentication.service.RefreshTokenService;
import com.securegate.authentication.service.SessionService;
import com.securegate.common.error.ConflictException;
import com.securegate.common.error.UnauthorizedException;
import com.securegate.identity.domain.EmailVerificationToken;
import com.securegate.identity.domain.User;
import com.securegate.identity.domain.UserStatus;
import com.securegate.identity.dto.LoginRequest;
import com.securegate.identity.dto.RegisterRequest;
import com.securegate.identity.dto.UserDto;
import com.securegate.identity.repository.EmailVerificationTokenRepository;
import com.securegate.identity.repository.PasswordResetTokenRepository;
import com.securegate.identity.repository.UserEmailRepository;
import com.securegate.identity.repository.UserRepository;
import com.securegate.notification.EmailService;
import com.securegate.tenant.domain.Organization;
import com.securegate.tenant.domain.OrganizationMembership;
import com.securegate.tenant.domain.Role;
import com.securegate.tenant.dto.OrganizationDto;
import com.securegate.tenant.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserEmailRepository userEmailRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TenantService tenantService;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private SessionService sessionService;
    @Mock private EmailService emailService;
    @Mock private AuditService auditService;

    private IdentityService identityService;

    @BeforeEach
    void setUp() {
        identityService = new IdentityService(
                userRepository,
                userEmailRepository,
                emailVerificationTokenRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                tenantService,
                jwtService,
                refreshTokenService,
                sessionService,
                emailService,
                auditService
        );
    }

    @Test
    @DisplayName("Should successfully register user and normalize email")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest("Alice Test", "  ALICE@Example.COM  ", "SecurePassword123!", "Acme Corp");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("$argon2id$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        Organization org = new Organization("Acme Corp", "acme-corp");
        org.setId(UUID.randomUUID());
        when(tenantService.createOrganization(eq("Acme Corp"), any(User.class))).thenReturn(org);

        UserDto userDto = identityService.register(request);

        assertThat(userDto).isNotNull();
        assertThat(userDto.email()).isEqualTo("alice@example.com");
        assertThat(userDto.status()).isEqualTo(UserStatus.PENDING_VERIFICATION);

        verify(emailService).sendVerificationEmail(eq("alice@example.com"), anyString());
        verify(auditService).logEvent(any(), any(), eq("USER_REGISTER"), eq("USER"), anyString(), eq("SUCCESS"), anyString(), anyString());
    }

    @Test
    @DisplayName("Should reject duplicate registration with 409 Conflict")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Bob Test", "bob@example.com", "SecurePassword123!", "Acme Corp");
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThatThrownBy(() -> identityService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should reject invalid login and lock account after 5 failed attempts")
    void testAccountLockoutAfterFiveFailures() {
        LoginRequest request = new LoginRequest("victim@example.com", "WrongPassword!");
        User user = new User("Victim", "victim@example.com", "$argon2id$hash");
        user.setId(UUID.randomUUID());

        when(userRepository.findByEmail("victim@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword!", "$argon2id$hash")).thenReturn(false);

        // Attempt 1 to 4: failed login increments count
        for (int i = 1; i <= 4; i++) {
            assertThatThrownBy(() -> identityService.login(request, "device", "127.0.0.1", null))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid email or password");
            assertThat(user.getFailedLoginAttempts()).isEqualTo(i);
        }

        // 5th attempt: triggers account lockout
        assertThatThrownBy(() -> identityService.login(request, "device", "127.0.0.1", null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.isAccountNonLocked()).isFalse();

        // 6th attempt: immediately rejected due to lockout
        assertThatThrownBy(() -> identityService.login(request, "device", "127.0.0.1", null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("temporarily locked");
    }

    // -----------------------------------------------------------------------
    // verifyEmail
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Should mark user as ACTIVE and email verified on valid token")
    void testVerifyEmailSuccess() {
        User user = new User("Alice", "alice@example.com", "hash");
        user.setId(UUID.randomUUID());

        String rawToken = "raw-verification-token";
        String tokenHash = com.securegate.common.SecurityUtils.sha256Hex(rawToken);

        com.securegate.identity.domain.EmailVerificationToken evToken =
                new com.securegate.identity.domain.EmailVerificationToken(
                        user.getId(), tokenHash, java.time.Instant.now().plusSeconds(3600));

        when(emailVerificationTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(evToken));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emailVerificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.logEvent(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(null);

        identityService.verifyEmail(rawToken);

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getStatus()).isEqualTo(com.securegate.identity.domain.UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should reject an already-used verification token")
    void testVerifyEmailAlreadyUsedToken() {
        User user = new User("Alice", "alice@example.com", "hash");
        user.setId(UUID.randomUUID());

        String rawToken = "used-token";
        String tokenHash = com.securegate.common.SecurityUtils.sha256Hex(rawToken);

        com.securegate.identity.domain.EmailVerificationToken evToken =
                new com.securegate.identity.domain.EmailVerificationToken(
                        user.getId(), tokenHash, java.time.Instant.now().plusSeconds(3600));
        evToken.markUsed();

        when(emailVerificationTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(evToken));

        assertThatThrownBy(() -> identityService.verifyEmail(rawToken))
                .isInstanceOf(com.securegate.common.error.ConflictException.class)
                .hasMessageContaining("already been used");
    }

    // -----------------------------------------------------------------------
    // forgotPassword
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Should send reset email for a registered user")
    void testForgotPasswordSendsEmail() {
        User user = new User("Alice", "alice@example.com", "hash");
        user.setId(UUID.randomUUID());

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.logEvent(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(null);

        identityService.forgotPassword("alice@example.com");

        verify(passwordResetTokenRepository).deleteByUserId(user.getId());
        verify(emailService).sendPasswordResetEmail(eq("alice@example.com"), anyString());
    }

    @Test
    @DisplayName("Should silently ignore unknown email to prevent user enumeration")
    void testForgotPasswordUnknownEmailIsNoOp() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        identityService.forgotPassword("unknown@example.com");

        verifyNoInteractions(emailService);
        verifyNoInteractions(passwordResetTokenRepository);
    }

    // -----------------------------------------------------------------------
    // changePassword
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Should change password and revoke all sessions on success")
    void testChangePasswordSuccess() {
        User user = new User("Alice", "alice@example.com", "$argon2id$oldhash");
        user.setId(UUID.randomUUID());

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123!", "$argon2id$oldhash")).thenReturn(true);
        when(passwordEncoder.encode("NewPass123!")).thenReturn("$argon2id$newhash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.logEvent(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(null);

        identityService.changePassword(user.getId(), "OldPass123!", "NewPass123!");

        assertThat(user.getPasswordHash()).isEqualTo("$argon2id$newhash");
        verify(refreshTokenService).revokeAllForUser(user.getId());
    }

    @Test
    @DisplayName("Should reject change password when current password is incorrect")
    void testChangePasswordWrongCurrent() {
        User user = new User("Alice", "alice@example.com", "$argon2id$hash");
        user.setId(UUID.randomUUID());

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass!", "$argon2id$hash")).thenReturn(false);

        assertThatThrownBy(() -> identityService.changePassword(user.getId(), "WrongPass!", "NewPass123!"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Current password does not match");

        verifyNoMoreInteractions(refreshTokenService);
    }
}
