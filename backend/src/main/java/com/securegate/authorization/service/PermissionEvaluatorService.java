package com.securegate.authorization.service;

import com.securegate.tenant.context.TenantContext;
import com.securegate.tenant.domain.OrganizationMembership;
import com.securegate.tenant.repository.OrganizationMembershipRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service("authz")
public class PermissionEvaluatorService {

    private final OrganizationMembershipRepository membershipRepository;

    public PermissionEvaluatorService(OrganizationMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public boolean hasPermission(Authentication authentication, UUID organizationId, String requiredPermission) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return false;
        }

        // Platform administrators bypass organization-level permission barriers
        if (TenantContext.isPlatformAdmin() || authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PLATFORM_ADMIN"))) {
            return true;
        }

        UUID userId;
        try {
            userId = UUID.fromString(authentication.getPrincipal().toString());
        } catch (IllegalArgumentException e) {
            return false;
        }

        // Always validate server-side against current database records!
        Optional<OrganizationMembership> membershipOpt = membershipRepository.findByOrganizationIdAndUserId(organizationId, userId);
        if (membershipOpt.isEmpty()) {
            return false;
        }

        OrganizationMembership membership = membershipOpt.get();
        return membership.getRole().getPermissions().stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(requiredPermission));
    }

    public boolean isOrgOwner(Authentication authentication, UUID organizationId) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return false;
        }
        if (TenantContext.isPlatformAdmin() || authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PLATFORM_ADMIN"))) {
            return true;
        }
        try {
            UUID userId = UUID.fromString(authentication.getPrincipal().toString());
            return membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                    .map(m -> "ORG_OWNER".equals(m.getRole().getName()))
                    .orElse(false);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
