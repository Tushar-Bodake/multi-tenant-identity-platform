package com.securegate.tenant.web;

import com.securegate.common.error.ConflictException;
import com.securegate.common.error.ResourceNotFoundException;
import com.securegate.common.error.UnauthorizedException;
import com.securegate.identity.domain.User;
import com.securegate.identity.service.IdentityService;
import com.securegate.tenant.domain.Invitation;
import com.securegate.tenant.domain.Organization;
import com.securegate.tenant.domain.OrganizationMembership;
import com.securegate.tenant.dto.OrganizationDto;
import com.securegate.tenant.repository.OrganizationMembershipRepository;
import com.securegate.tenant.repository.OrganizationRepository;
import com.securegate.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations", description = "Multi-tenant organization, membership, and invitation management")
public class TenantController {

    private final TenantService tenantService;
    private final IdentityService identityService;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;

    public record CreateOrgRequest(
            @NotBlank @Size(min = 2, max = 255) String name
    ) {}

    public record UpdateOrgRequest(
            @NotBlank @Size(min = 2, max = 255) String name,
            boolean enforceMfa,
            String allowedDomains
    ) {}

    public record InviteMemberRequest(
            @NotBlank @Email String email,
            @NotBlank String role
    ) {}

    public record UpdateRoleRequest(
            @NotBlank String role
    ) {}

    public record AcceptInvitationRequest(
            @NotBlank String token
    ) {}

    public record TransferOwnershipRequest(
            @jakarta.validation.constraints.NotNull UUID newOwnerUserId,
            @NotBlank String currentPassword
    ) {}

    public record MemberDto(
            UUID userId,
            String fullName,
            String email,
            String role,
            boolean isCurrentUser
    ) {}

    public TenantController(
            TenantService tenantService,
            IdentityService identityService,
            OrganizationRepository organizationRepository,
            OrganizationMembershipRepository membershipRepository) {
        this.tenantService = tenantService;
        this.identityService = identityService;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new organization")
    public ResponseEntity<OrganizationDto> createOrganization(
            @Valid @RequestBody CreateOrgRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        User user = identityService.getUserById(userId);
        Organization org = tenantService.createOrganization(request.name(), user);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new OrganizationDto(org.getId(), org.getName(), org.getSlug(), org.getStatus(), "ORG_OWNER")
        );
    }

    @GetMapping
    @Operation(summary = "List all organizations the authenticated user belongs to")
    public ResponseEntity<List<OrganizationDto>> listUserOrganizations(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        return ResponseEntity.ok(tenantService.getUserOrganizations(userId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.hasPermission(authentication, #id, 'organization:read')")
    @Operation(summary = "Get organization details")
    public ResponseEntity<Organization> getOrganization(@PathVariable UUID id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        return ResponseEntity.ok(org);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.hasPermission(authentication, #id, 'organization:update')")
    @Operation(summary = "Update organization settings")
    public ResponseEntity<Organization> updateOrganization(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrgRequest request) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        org.setName(request.name());
        org.setEnforceMfa(request.enforceMfa());
        org.setAllowedDomains(request.allowedDomains());
        return ResponseEntity.ok(organizationRepository.save(org));
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("@authz.hasPermission(authentication, #id, 'members:read')")
    @Operation(summary = "List organization members")
    public ResponseEntity<List<MemberDto>> listMembers(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID currentUserId = UUID.fromString(authentication.getPrincipal().toString());
        List<OrganizationMembership> memberships = membershipRepository.findByOrganizationId(id);

        List<MemberDto> memberDtos = new ArrayList<>();
        for (OrganizationMembership m : memberships) {
            User u = identityService.getUserById(m.getUserId());
            memberDtos.add(new MemberDto(
                    u.getId(),
                    u.getFullName(),
                    u.getEmail(),
                    m.getRole().getName(),
                    u.getId().equals(currentUserId)
            ));
        }

        return ResponseEntity.ok(memberDtos);
    }

    @PostMapping("/{id}/invitations")
    @PreAuthorize("@authz.hasPermission(authentication, #id, 'members:invite')")
    @Operation(summary = "Invite a new member to the organization")
    public ResponseEntity<Map<String, Object>> inviteMember(
            @PathVariable UUID id,
            @Valid @RequestBody InviteMemberRequest request,
            Authentication authentication) {
        UUID inviterId = UUID.fromString(authentication.getPrincipal().toString());
        User inviter = identityService.getUserById(inviterId);

        Invitation invitation = tenantService.inviteMember(id, request.email(), request.role(), inviter);

        Map<String, Object> response = new HashMap<>();
        response.put("id", invitation.getId());
        response.put("email", invitation.getEmail());
        response.put("role", invitation.getRole().getName());
        response.put("expiresAt", invitation.getExpiresAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/invitations/accept")
    @Operation(summary = "Accept an invitation token and join the organization")
    public ResponseEntity<OrganizationDto> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        User user = identityService.getUserById(userId);

        OrganizationMembership membership = tenantService.acceptInvitation(request.token(), user);
        Organization org = organizationRepository.findById(membership.getOrganizationId()).orElseThrow();

        return ResponseEntity.ok(new OrganizationDto(org.getId(), org.getName(), org.getSlug(), org.getStatus(), membership.getRole().getName()));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("@authz.hasPermission(authentication, #id, 'members:remove')")
    @Operation(summary = "Remove a member from the organization")
    public ResponseEntity<Map<String, String>> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            Authentication authentication) {
        UUID actorId = UUID.fromString(authentication.getPrincipal().toString());
        User actor = identityService.getUserById(actorId);

        tenantService.removeMember(id, userId, actor);
        return ResponseEntity.ok(Map.of("message", "Member removed successfully"));
    }

    @PatchMapping("/{id}/members/{userId}/role")
    @PreAuthorize("@authz.hasPermission(authentication, #id, 'roles:assign')")
    @Operation(summary = "Change a member's role in the organization")
    public ResponseEntity<Map<String, String>> updateMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateRoleRequest request,
            Authentication authentication) {
        UUID actorId = UUID.fromString(authentication.getPrincipal().toString());
        User actor = identityService.getUserById(actorId);

        tenantService.updateMemberRole(id, userId, request.role(), actor);
        return ResponseEntity.ok(Map.of("message", "Member role updated successfully"));
    }

    @PostMapping("/{id}/transfer-ownership")
    @PreAuthorize("@authz.isOrgOwner(authentication, #id)")
    @Operation(summary = "Transfer organization ownership to another member")
    public ResponseEntity<Map<String, String>> transferOwnership(
            @PathVariable UUID id,
            @Valid @RequestBody TransferOwnershipRequest request,
            Authentication authentication) {
        UUID currentOwnerId = UUID.fromString(authentication.getPrincipal().toString());
        User currentOwner = identityService.getUserById(currentOwnerId);

        if (currentOwnerId.equals(request.newOwnerUserId())) {
            throw new ConflictException("You are already the owner of this organization");
        }

        // Promote new owner and set current owner to admin
        tenantService.updateMemberRole(id, request.newOwnerUserId(), "ORG_OWNER", currentOwner);
        tenantService.updateMemberRole(id, currentOwnerId, "ORG_ADMIN", currentOwner);

        return ResponseEntity.ok(Map.of("message", "Ownership transferred successfully"));
    }
}
