package com.securegate.tenant.web;

import com.securegate.authorization.service.PermissionEvaluatorService;
import com.securegate.identity.domain.User;
import com.securegate.identity.service.IdentityService;
import com.securegate.tenant.domain.Organization;
import com.securegate.tenant.repository.OrganizationMembershipRepository;
import com.securegate.tenant.repository.OrganizationRepository;
import com.securegate.tenant.service.TenantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PermissionEvaluatorService permissionEvaluatorService;

    @MockBean
    private TenantService tenantService;

    @MockBean
    private OrganizationRepository organizationRepository;

    @MockBean
    private OrganizationMembershipRepository membershipRepository;

    @MockBean
    private IdentityService identityService;

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = {"MEMBER"})
    @DisplayName("Negative Test: Deny member invite when user lacks members:invite permission")
    void testDenyInviteWithoutPermission() throws Exception {
        UUID orgId = UUID.randomUUID();
        when(permissionEvaluatorService.hasPermission(any(), eq(orgId), eq("members:invite"))).thenReturn(false);

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"guest@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = {"ORG_ADMIN"})
    @DisplayName("Negative Test: Deny ownership transfer when user is not the ORG_OWNER")
    void testDenyTransferOwnershipIfNotOwner() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID newOwnerId = UUID.randomUUID();
        when(permissionEvaluatorService.isOrgOwner(any(), eq(orgId))).thenReturn(false);

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/transfer-ownership")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newOwnerUserId\":\"" + newOwnerId + "\",\"currentPassword\":\"Password123!\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001")
    @DisplayName("Negative Test: Deny cross-tenant organization detail access")
    void testDenyCrossTenantAccess() throws Exception {
        UUID crossTenantOrgId = UUID.randomUUID();
        // Permission evaluator rejects because user belongs to a different tenant!
        when(permissionEvaluatorService.hasPermission(any(), eq(crossTenantOrgId), eq("organization:read"))).thenReturn(false);

        mockMvc.perform(get("/api/v1/organizations/" + crossTenantOrgId))
                .andExpect(status().isForbidden());
    }
}
