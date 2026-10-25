package com.securegate.authorization.service;

import com.securegate.tenant.context.TenantContext;
import com.securegate.tenant.domain.OrganizationMembership;
import com.securegate.tenant.domain.Permission;
import com.securegate.tenant.domain.Role;
import com.securegate.tenant.repository.OrganizationMembershipRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionEvaluatorServiceTest {

    @Mock
    private OrganizationMembershipRepository membershipRepository;

    private PermissionEvaluatorService evaluatorService;

    @BeforeEach
    void setUp() {
        evaluatorService = new PermissionEvaluatorService(membershipRepository);
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Platform administrator should bypass organization-level permission checks")
    void testPlatformAdminBypass() {
        UUID orgId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                UUID.randomUUID().toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"))
        );

        assertTrue(evaluatorService.hasPermission(auth, orgId, "organization:delete"));
        assertTrue(evaluatorService.isOrgOwner(auth, orgId));
    }

    @Test
    @DisplayName("Unauthenticated request must be denied by default")
    void testUnauthenticatedDenied() {
        UUID orgId = UUID.randomUUID();
        assertFalse(evaluatorService.hasPermission(null, orgId, "organization:read"));
        assertFalse(evaluatorService.isOrgOwner(null, orgId));
    }

    @Test
    @DisplayName("User with granted permission in target tenant should be authorized")
    void testHasPermissionGranted() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Role role = new Role(UUID.randomUUID(), "ORG_OWNER", "Owner", true);
        Permission p1 = new Permission(UUID.randomUUID(), "organization:read", "Read Org");
        Permission p2 = new Permission(UUID.randomUUID(), "members:invite", "Invite members");
        role.setPermissions(Set.of(p1, p2));

        OrganizationMembership membership = new OrganizationMembership(orgId, userId, role);

        when(membershipRepository.findByOrganizationIdAndUserId(orgId, userId))
                .thenReturn(Optional.of(membership));

        Authentication auth = new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());

        assertTrue(evaluatorService.hasPermission(auth, orgId, "members:invite"));
        assertTrue(evaluatorService.hasPermission(auth, orgId, "organization:read"));
    }

    @Test
    @DisplayName("User lacking permission in target tenant must be denied")
    void testLacksPermissionDenied() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Role role = new Role(UUID.randomUUID(), "MEMBER", "Standard Member", true);
        Permission p1 = new Permission(UUID.randomUUID(), "organization:read", "Read Org");
        role.setPermissions(Set.of(p1));

        OrganizationMembership membership = new OrganizationMembership(orgId, userId, role);

        when(membershipRepository.findByOrganizationIdAndUserId(orgId, userId))
                .thenReturn(Optional.of(membership));

        Authentication auth = new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());

        assertFalse(evaluatorService.hasPermission(auth, orgId, "members:remove"));
    }

    @Test
    @DisplayName("Cross-tenant access attempt must be rejected when user has no membership in tenant")
    void testCrossTenantAccessDenied() {
        UUID userId = UUID.randomUUID();
        UUID foreignOrgId = UUID.randomUUID();

        when(membershipRepository.findByOrganizationIdAndUserId(foreignOrgId, userId))
                .thenReturn(Optional.empty());

        Authentication auth = new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());

        assertFalse(evaluatorService.hasPermission(auth, foreignOrgId, "organization:read"));
        assertFalse(evaluatorService.isOrgOwner(auth, foreignOrgId));
    }

    @Test
    @DisplayName("isOrgOwner should return true only for ORG_OWNER role")
    void testIsOrgOwnerCheck() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Role ownerRole = new Role(UUID.randomUUID(), "ORG_OWNER", "Owner", true);
        Role adminRole = new Role(UUID.randomUUID(), "ORG_ADMIN", "Admin", true);

        OrganizationMembership ownerMembership = new OrganizationMembership(orgId, userId, ownerRole);
        OrganizationMembership adminMembership = new OrganizationMembership(orgId, userId, adminRole);

        Authentication auth = new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());

        when(membershipRepository.findByOrganizationIdAndUserId(orgId, userId))
                .thenReturn(Optional.of(ownerMembership));
        assertTrue(evaluatorService.isOrgOwner(auth, orgId));

        when(membershipRepository.findByOrganizationIdAndUserId(orgId, userId))
                .thenReturn(Optional.of(adminMembership));
        assertFalse(evaluatorService.isOrgOwner(auth, orgId));
    }
}
