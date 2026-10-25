package com.securegate.common.config;

import com.securegate.audit.service.AuditService;
import com.securegate.identity.domain.User;
import com.securegate.identity.domain.UserEmail;
import com.securegate.identity.domain.UserStatus;
import com.securegate.identity.repository.UserEmailRepository;
import com.securegate.identity.repository.UserRepository;
import com.securegate.tenant.domain.Organization;
import com.securegate.tenant.domain.OrganizationMembership;
import com.securegate.tenant.domain.Permission;
import com.securegate.tenant.domain.Role;
import com.securegate.tenant.repository.OrganizationMembershipRepository;
import com.securegate.tenant.repository.OrganizationRepository;
import com.securegate.tenant.repository.PermissionRepository;
import com.securegate.tenant.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final UserEmailRepository userEmailRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public DataInitializer(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserRepository userRepository,
            UserEmailRepository userEmailRepository,
            OrganizationRepository organizationRepository,
            OrganizationMembershipRepository membershipRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.userEmailRepository = userEmailRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing system roles, permissions, and default admin user if not present...");

        // 1. Permissions
        Map<String, String> perms = Map.of(
                "organization:read", "View organization details and configuration",
                "organization:update", "Update organization settings and authentication policy",
                "organization:delete", "Delete organization and transfer ownership",
                "members:read", "View organization members and invitations",
                "members:invite", "Invite new members to the organization",
                "members:remove", "Remove members and revoke pending invitations",
                "roles:assign", "Assign and change member roles",
                "applications:manage", "Register and manage client applications and credentials",
                "audit:read", "View tenant-scoped audit events and verify audit chain",
                "security:manage", "Configure organization security settings and passkey policies"
        );

        Map<String, Permission> permEntities = new HashMap<>();
        for (Map.Entry<String, String> entry : perms.entrySet()) {
            Permission p = permissionRepository.findByName(entry.getKey())
                    .orElseGet(() -> permissionRepository.save(new Permission(UUID.randomUUID(), entry.getKey(), entry.getValue())));
            permEntities.put(entry.getKey(), p);
        }

        Permission platformAdminPerm = permissionRepository.findByName("platform:admin")
                .orElseGet(() -> permissionRepository.save(new Permission(UUID.randomUUID(), "platform:admin", "Platform administration")));
        permEntities.put("platform:admin", platformAdminPerm);

        // 2. Roles
        Role ownerRole = roleRepository.findByName("ORG_OWNER")
                .orElseGet(() -> {
                    Role r = new Role(UUID.randomUUID(), "ORG_OWNER", "Organization Owner", true);
                    r.setPermissions(new HashSet<>(permEntities.values()));
                    return roleRepository.save(r);
                });

        Role adminRole = roleRepository.findByName("ORG_ADMIN")
                .orElseGet(() -> {
                    Role r = new Role(UUID.randomUUID(), "ORG_ADMIN", "Organization Administrator", true);
                    Set<Permission> set = new HashSet<>();
                    set.add(permEntities.get("organization:read"));
                    set.add(permEntities.get("organization:update"));
                    set.add(permEntities.get("members:read"));
                    set.add(permEntities.get("members:invite"));
                    set.add(permEntities.get("members:remove"));
                    set.add(permEntities.get("roles:assign"));
                    set.add(permEntities.get("applications:manage"));
                    set.add(permEntities.get("audit:read"));
                    r.setPermissions(set);
                    return roleRepository.save(r);
                });

        Role devRole = roleRepository.findByName("DEVELOPER")
                .orElseGet(() -> {
                    Role r = new Role(UUID.randomUUID(), "DEVELOPER", "Developer", true);
                    Set<Permission> set = new HashSet<>();
                    set.add(permEntities.get("organization:read"));
                    set.add(permEntities.get("applications:manage"));
                    r.setPermissions(set);
                    return roleRepository.save(r);
                });

        Role memberRole = roleRepository.findByName("MEMBER")
                .orElseGet(() -> {
                    Role r = new Role(UUID.randomUUID(), "MEMBER", "Member", true);
                    Set<Permission> set = new HashSet<>();
                    set.add(permEntities.get("organization:read"));
                    r.setPermissions(set);
                    return roleRepository.save(r);
                });

        Role platformAdminRole = roleRepository.findByName("PLATFORM_ADMIN")
                .orElseGet(() -> {
                    Role r = new Role(UUID.randomUUID(), "PLATFORM_ADMIN", "Platform Administrator", true);
                    Set<Permission> set = new HashSet<>();
                    set.add(platformAdminPerm);
                    r.setPermissions(set);
                    return roleRepository.save(r);
                });

        // 3. Default Platform Administrator user
        String adminEmail = "admin@securegate.local";
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User("System Administrator", adminEmail, passwordEncoder.encode("AdminPassword123!"));
            admin.setEmailVerified(true);
            admin.setStatus(UserStatus.ACTIVE);
            admin = userRepository.save(admin);

            UserEmail adminUserEmail = new UserEmail(admin.getId(), adminEmail, true, true);
            userEmailRepository.save(adminUserEmail);

            Organization platformOrg = organizationRepository.findBySlug("securegate-hq")
                    .orElseGet(() -> organizationRepository.save(new Organization("SecureGate Platform HQ", "securegate-hq")));

            OrganizationMembership membership = new OrganizationMembership(platformOrg.getId(), admin.getId(), platformAdminRole);
            membershipRepository.save(membership);

            auditService.logEvent(admin.getId(), platformOrg.getId(), "SYSTEM_INIT", "PLATFORM",
                    admin.getId().toString(), "SUCCESS", UUID.randomUUID().toString(),
                    "{\"adminEmail\":\"" + adminEmail + "\"}");

            log.info("Created default platform administrator: admin@securegate.local / AdminPassword123!");
        }
    }
}
