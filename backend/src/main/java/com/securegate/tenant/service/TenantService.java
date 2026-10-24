package com.securegate.tenant.service;

import com.securegate.audit.service.AuditService;
import com.securegate.common.SecurityUtils;
import com.securegate.common.error.ConflictException;
import com.securegate.common.error.ResourceNotFoundException;
import com.securegate.common.error.TenantAccessDeniedException;
import com.securegate.identity.domain.User;
import com.securegate.identity.repository.UserRepository;
import com.securegate.notification.EmailService;
import com.securegate.tenant.domain.*;
import com.securegate.tenant.dto.OrganizationDto;
import com.securegate.tenant.repository.InvitationRepository;
import com.securegate.tenant.repository.OrganizationMembershipRepository;
import com.securegate.tenant.repository.OrganizationRepository;
import com.securegate.tenant.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TenantService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AuditService auditService;

    public TenantService(
            OrganizationRepository organizationRepository,
            OrganizationMembershipRepository membershipRepository,
            RoleRepository roleRepository,
            InvitationRepository invitationRepository,
            UserRepository userRepository,
            EmailService emailService,
            AuditService auditService) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    @Transactional
    public Organization createOrganization(String name, User owner) {
        String baseSlug = name.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
        if (baseSlug.isBlank()) {
            baseSlug = "org";
        }
        String slug = baseSlug;
        int counter = 1;
        while (organizationRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        Organization org = new Organization(name, slug);
        org = organizationRepository.save(org);

        Role ownerRole = roleRepository.findByName("ORG_OWNER")
                .orElseThrow(() -> new IllegalStateException("ORG_OWNER role not found in database"));

        OrganizationMembership membership = new OrganizationMembership(org.getId(), owner.getId(), ownerRole);
        membershipRepository.save(membership);

        auditService.logEvent(owner.getId(), org.getId(), "ORGANIZATION_CREATE", "ORGANIZATION",
                org.getId().toString(), "SUCCESS", UUID.randomUUID().toString(), "{\"name\":\"" + name + "\"}");

        return org;
    }

    @Transactional(readOnly = true)
    public List<OrganizationDto> getUserOrganizations(UUID userId) {
        List<OrganizationMembership> memberships = membershipRepository.findByUserId(userId);
        return memberships.stream().map(m -> {
            Organization org = organizationRepository.findById(m.getOrganizationId()).orElse(null);
            if (org == null) return null;
            return new OrganizationDto(org.getId(), org.getName(), org.getSlug(), org.getStatus(), m.getRole().getName());
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrganizationDto validateUserMembership(UUID userId, UUID organizationId) {
        OrganizationMembership membership = membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new TenantAccessDeniedException("You do not belong to this organization"));
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        return new OrganizationDto(org.getId(), org.getName(), org.getSlug(), org.getStatus(), membership.getRole().getName());
    }

    @Transactional(readOnly = true)
    public OrganizationMembership getMembership(UUID organizationId, UUID userId) {
        return membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new TenantAccessDeniedException("User is not a member of organization " + organizationId));
    }

    @Transactional
    public Invitation inviteMember(UUID organizationId, String email, String roleName, User inviter) {
        String normalizedEmail = SecurityUtils.normalizeEmail(email);

        if (invitationRepository.existsByOrganizationIdAndEmailAndAcceptedAtIsNull(organizationId, normalizedEmail)) {
            throw new ConflictException("A pending invitation for " + normalizedEmail + " already exists");
        }

        userRepository.findByEmail(normalizedEmail).ifPresent(existingUser -> {
            if (membershipRepository.existsByOrganizationIdAndUserId(organizationId, existingUser.getId())) {
                throw new ConflictException("User is already a member of this organization");
            }
        });

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        // Generate high entropy single-use token and store SHA-256 hash
        String rawToken = SecurityUtils.generateSecureToken(32);
        String tokenHash = SecurityUtils.sha256Hex(rawToken);
        Instant expiresAt = Instant.now().plusSeconds(7 * 24 * 3600); // 7 days

        Invitation invitation = new Invitation(organizationId, normalizedEmail, role, tokenHash, inviter.getId(), expiresAt);
        invitation = invitationRepository.save(invitation);

        Organization org = organizationRepository.findById(organizationId).orElseThrow();
        emailService.sendInvitationEmail(normalizedEmail, org.getName(), rawToken, roleName);

        auditService.logEvent(inviter.getId(), organizationId, "MEMBER_INVITE", "INVITATION",
                invitation.getId().toString(), "SUCCESS", UUID.randomUUID().toString(),
                "{\"email\":\"" + normalizedEmail + "\",\"role\":\"" + roleName + "\"}");

        return invitation;
    }

    @Transactional
    public OrganizationMembership acceptInvitation(String rawToken, User user) {
        String tokenHash = SecurityUtils.sha256Hex(rawToken);
        Invitation invitation = invitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired invitation token"));

        if (!invitation.isValid()) {
            throw new ConflictException("Invitation has already been accepted or expired");
        }

        // Verify email matches
        if (!invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new TenantAccessDeniedException("This invitation was sent to " + invitation.getEmail() + ", not " + user.getEmail());
        }

        // Check if already a member
        Optional<OrganizationMembership> existing = membershipRepository.findByOrganizationIdAndUserId(invitation.getOrganizationId(), user.getId());
        if (existing.isPresent()) {
            invitation.markAccepted();
            invitationRepository.save(invitation);
            return existing.get();
        }

        OrganizationMembership membership = new OrganizationMembership(invitation.getOrganizationId(), user.getId(), invitation.getRole());
        membership = membershipRepository.save(membership);

        invitation.markAccepted();
        invitationRepository.save(invitation);

        auditService.logEvent(user.getId(), invitation.getOrganizationId(), "MEMBER_ACCEPT", "MEMBERSHIP",
                membership.getId().toString(), "SUCCESS", UUID.randomUUID().toString(),
                "{\"email\":\"" + user.getEmail() + "\"}");

        return membership;
    }

    @Transactional
    public void removeMember(UUID organizationId, UUID targetUserId, User actor) {
        OrganizationMembership membership = membershipRepository.findByOrganizationIdAndUserId(organizationId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in organization"));

        // Safeguard: Protect against removing the last organization owner!
        if ("ORG_OWNER".equals(membership.getRole().getName())) {
            long ownerCount = membershipRepository.countByOrganizationIdAndRole_Name(organizationId, "ORG_OWNER");
            if (ownerCount <= 1) {
                throw new ConflictException("Cannot remove the last owner of the organization");
            }
        }

        membershipRepository.delete(membership);

        auditService.logEvent(actor.getId(), organizationId, "MEMBER_REMOVE", "USER",
                targetUserId.toString(), "SUCCESS", UUID.randomUUID().toString(), "{}");
    }

    @Transactional
    public void updateMemberRole(UUID organizationId, UUID targetUserId, String newRoleName, User actor) {
        OrganizationMembership membership = membershipRepository.findByOrganizationIdAndUserId(organizationId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in organization"));

        // Safeguard: Demoting an owner requires that another owner exists
        if ("ORG_OWNER".equals(membership.getRole().getName()) && !"ORG_OWNER".equals(newRoleName)) {
            long ownerCount = membershipRepository.countByOrganizationIdAndRole_Name(organizationId, "ORG_OWNER");
            if (ownerCount <= 1) {
                throw new ConflictException("Cannot demote the last owner. Promote another member to owner first.");
            }
        }

        Role newRole = roleRepository.findByName(newRoleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + newRoleName));

        membership.setRole(newRole);
        membershipRepository.save(membership);

        auditService.logEvent(actor.getId(), organizationId, "ROLE_CHANGE", "USER",
                targetUserId.toString(), "SUCCESS", UUID.randomUUID().toString(),
                "{\"newRole\":\"" + newRoleName + "\"}");
    }
}
