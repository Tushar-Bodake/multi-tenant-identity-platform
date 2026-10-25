package com.securegate.tenant.service;

import com.securegate.audit.service.AuditService;
import com.securegate.common.error.ConflictException;
import com.securegate.common.error.ResourceNotFoundException;
import com.securegate.common.error.TenantAccessDeniedException;
import com.securegate.identity.domain.User;
import com.securegate.identity.repository.UserRepository;
import com.securegate.notification.EmailService;
import com.securegate.tenant.domain.*;
import com.securegate.tenant.repository.InvitationRepository;
import com.securegate.tenant.repository.OrganizationMembershipRepository;
import com.securegate.tenant.repository.OrganizationRepository;
import com.securegate.tenant.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private OrganizationMembershipRepository membershipRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private InvitationRepository invitationRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private AuditService auditService;

    private TenantService tenantService;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID orgId   = UUID.randomUUID();

    private User owner;
    private Role ownerRole;

    @BeforeEach
    void setUp() {
        tenantService = new TenantService(
                organizationRepository,
                membershipRepository,
                roleRepository,
                invitationRepository,
                userRepository,
                emailService,
                auditService
        );

        owner = new User("Alice Owner", "alice@example.com", "$argon2id$hash");
        owner.setId(ownerId);

        ownerRole = new Role(UUID.fromString("22222222-0000-0000-0000-000000000002"), "ORG_OWNER", "Organization Owner", true);
        ownerRole.setPermissions(Set.of());
    }

    // -----------------------------------------------------------------------
    // createOrganization
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Should create organization, generate slug, and add owner membership")
    void testCreateOrganizationSuccess() {
        when(organizationRepository.existsBySlug(anyString())).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> {
            Organization o = inv.getArgument(0);
            o.setId(orgId);
            return o;
        });
        when(roleRepository.findByName("ORG_OWNER")).thenReturn(Optional.of(ownerRole));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.logEvent(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(null);

        Organization result = tenantService.createOrganization("Acme Corp", owner);

        assertThat(result.getName()).isEqualTo("Acme Corp");
        assertThat(result.getSlug()).isEqualTo("acme-corp");

        ArgumentCaptor<OrganizationMembership> membershipCaptor = ArgumentCaptor.forClass(OrganizationMembership.class);
        verify(membershipRepository).save(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue().getRole().getName()).isEqualTo("ORG_OWNER");
        assertThat(membershipCaptor.getValue().getUserId()).isEqualTo(ownerId);
    }

    @Test
    @DisplayName("Should append numeric suffix to slug when base slug already exists")
    void testCreateOrganizationSlugDeduplication() {
        // First call (base slug) taken; second call (slug-1) free
        when(organizationRepository.existsBySlug("acme")).thenReturn(true);
        when(organizationRepository.existsBySlug("acme-1")).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> {
            Organization o = inv.getArgument(0);
            o.setId(orgId);
            return o;
        });
        when(roleRepository.findByName("ORG_OWNER")).thenReturn(Optional.of(ownerRole));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.logEvent(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(null);

        Organization result = tenantService.createOrganization("Acme", owner);
        assertThat(result.getSlug()).isEqualTo("acme-1");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when ORG_OWNER role is absent from database")
    void testCreateOrganizationMissingOwnerRole() {
        when(organizationRepository.existsBySlug(anyString())).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> {
            Organization o = inv.getArgument(0);
            o.setId(orgId);
            return o;
        });
        when(roleRepository.findByName("ORG_OWNER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.createOrganization("No-Role Corp", owner))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ORG_OWNER role not found");
    }

    // -----------------------------------------------------------------------
    // inviteMember
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Should invite member, store hashed token, and send invitation email")
    void testInviteMemberSuccess() {
        Role memberRole = new Role(UUID.randomUUID(), "MEMBER", "Member", true);
        Organization org = new Organization("Acme Corp", "acme-corp");
        org.setId(orgId);

        when(invitationRepository.existsByOrganizationIdAndEmailAndAcceptedAtIsNull(orgId, "bob@example.com"))
                .thenReturn(false);
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("MEMBER")).thenReturn(Optional.of(memberRole));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> {
            Invitation i = inv.getArgument(0);
            i.setId(UUID.randomUUID());
            return i;
        });
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(auditService.logEvent(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(null);

        Invitation invitation = tenantService.inviteMember(orgId, "bob@example.com", "MEMBER", owner);

        assertThat(invitation.getEmail()).isEqualTo("bob@example.com");
        assertThat(invitation.getRole().getName()).isEqualTo("MEMBER");
        verify(emailService).sendInvitationEmail(eq("bob@example.com"), eq("Acme Corp"), anyString(), eq("MEMBER"));
    }

    @Test
    @DisplayName("Should reject duplicate pending invitation for same email")
    void testInviteMemberDuplicatePending() {
        when(invitationRepository.existsByOrganizationIdAndEmailAndAcceptedAtIsNull(orgId, "bob@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> tenantService.inviteMember(orgId, "bob@example.com", "MEMBER", owner))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("pending invitation");
    }

    @Test
    @DisplayName("Should reject invitation when user is already a member")
    void testInviteMemberAlreadyMember() {
        User existingUser = new User("Bob", "bob@example.com", null);
        existingUser.setId(UUID.randomUUID());

        when(invitationRepository.existsByOrganizationIdAndEmailAndAcceptedAtIsNull(orgId, "bob@example.com"))
                .thenReturn(false);
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(existingUser));
        when(membershipRepository.existsByOrganizationIdAndUserId(orgId, existingUser.getId())).thenReturn(true);

        assertThatThrownBy(() -> tenantService.inviteMember(orgId, "bob@example.com", "MEMBER", owner))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already a member");
    }

    // -----------------------------------------------------------------------
    // acceptInvitation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Should accept valid invitation and create membership")
    void testAcceptInvitationSuccess() {
        Role memberRole = new Role(UUID.randomUUID(), "MEMBER", "Member", true);
        // Use a real Invitation so isValid() and markAccepted() work
        Invitation invitation = new Invitation(orgId, "bob@example.com", memberRole,
                "dummy-hash", ownerId, Instant.now().plusSeconds(3600));

        User bob = new User("Bob", "bob@example.com", null);
        bob.setId(UUID.randomUUID());

        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(invitation));
        when(membershipRepository.findByOrganizationIdAndUserId(orgId, bob.getId())).thenReturn(Optional.empty());
        when(membershipRepository.save(any(OrganizationMembership.class))).thenAnswer(inv -> {
            OrganizationMembership m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });
        when(invitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.logEvent(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(null);

        OrganizationMembership membership = tenantService.acceptInvitation("raw-token", bob);

        assertThat(membership.getUserId()).isEqualTo(bob.getId());
        assertThat(invitation.getAcceptedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should reject invitation when invited email does not match user email")
    void testAcceptInvitationEmailMismatch() {
        Role memberRole = new Role(UUID.randomUUID(), "MEMBER", "Member", true);
        Invitation invitation = new Invitation(orgId, "alice@example.com", memberRole,
                "hash", ownerId, Instant.now().plusSeconds(3600));

        User wrongUser = new User("Wrong User", "wrong@example.com", null);
        wrongUser.setId(UUID.randomUUID());

        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> tenantService.acceptInvitation("raw-token", wrongUser))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("alice@example.com");
    }

    // -----------------------------------------------------------------------
    // removeMember
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Should prevent removing the last organization owner")
    void testRemoveLastOwnerPrevented() {
        Role memberOwnerRole = new Role(UUID.randomUUID(), "ORG_OWNER", "Owner", true);
        OrganizationMembership membership = new OrganizationMembership(orgId, ownerId, memberOwnerRole);

        when(membershipRepository.findByOrganizationIdAndUserId(orgId, ownerId))
                .thenReturn(Optional.of(membership));
        when(membershipRepository.countByOrganizationIdAndRole_Name(orgId, "ORG_OWNER")).thenReturn(1L);

        assertThatThrownBy(() -> tenantService.removeMember(orgId, ownerId, owner))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("last owner");
    }

    // -----------------------------------------------------------------------
    // updateMemberRole
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Should prevent demoting the last owner without another owner in place")
    void testDemoteLastOwnerPrevented() {
        Role ownerRoleInDb = new Role(UUID.randomUUID(), "ORG_OWNER", "Owner", true);
        Role memberRoleInDb = new Role(UUID.randomUUID(), "MEMBER", "Member", true);
        OrganizationMembership membership = new OrganizationMembership(orgId, ownerId, ownerRoleInDb);

        when(membershipRepository.findByOrganizationIdAndUserId(orgId, ownerId))
                .thenReturn(Optional.of(membership));
        when(membershipRepository.countByOrganizationIdAndRole_Name(orgId, "ORG_OWNER")).thenReturn(1L);

        assertThatThrownBy(() -> tenantService.updateMemberRole(orgId, ownerId, "MEMBER", owner))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("demote the last owner");
    }

    @Test
    @DisplayName("Should allow role change when multiple owners exist")
    void testDemoteOwnerAllowedWhenMultipleOwners() {
        Role ownerRoleInDb = new Role(UUID.randomUUID(), "ORG_OWNER", "Owner", true);
        Role adminRoleInDb = new Role(UUID.randomUUID(), "ORG_ADMIN", "Admin", true);
        UUID targetId = UUID.randomUUID();
        OrganizationMembership membership = new OrganizationMembership(orgId, targetId, ownerRoleInDb);

        when(membershipRepository.findByOrganizationIdAndUserId(orgId, targetId))
                .thenReturn(Optional.of(membership));
        when(membershipRepository.countByOrganizationIdAndRole_Name(orgId, "ORG_OWNER")).thenReturn(2L);
        when(roleRepository.findByName("ORG_ADMIN")).thenReturn(Optional.of(adminRoleInDb));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.logEvent(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(null);

        tenantService.updateMemberRole(orgId, targetId, "ORG_ADMIN", owner);

        assertThat(membership.getRole().getName()).isEqualTo("ORG_ADMIN");
    }
}
