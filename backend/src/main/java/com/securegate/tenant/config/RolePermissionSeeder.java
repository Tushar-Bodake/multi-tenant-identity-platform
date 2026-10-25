package com.securegate.tenant.config;

import com.securegate.tenant.domain.Permission;
import com.securegate.tenant.domain.Role;
import com.securegate.tenant.repository.PermissionRepository;
import com.securegate.tenant.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Seeds system roles and permissions when Flyway is disabled (local H2) or the
 * database is empty. Matches the IDs in V3__seed_roles_and_permissions.sql.
 */
@Component
public class RolePermissionSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RolePermissionSeeder.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RolePermissionSeeder(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (roleRepository.findByName("ORG_OWNER").isPresent()) {
            return;
        }

        Map<String, Permission> permissions = seedPermissions();
        seedRoles(permissions);
        log.info("Seeded system roles and permissions for local/dev database");
    }

    private Map<String, Permission> seedPermissions() {
        List<Permission> definitions = List.of(
                permission("11111111-0000-0000-0000-000000000001", "organization:read", "View organization details and configuration"),
                permission("11111111-0000-0000-0000-000000000002", "organization:update", "Update organization settings and authentication policy"),
                permission("11111111-0000-0000-0000-000000000003", "organization:delete", "Delete organization and transfer ownership"),
                permission("11111111-0000-0000-0000-000000000004", "members:read", "View organization members and invitations"),
                permission("11111111-0000-0000-0000-000000000005", "members:invite", "Invite new members to the organization"),
                permission("11111111-0000-0000-0000-000000000006", "members:remove", "Remove members and revoke pending invitations"),
                permission("11111111-0000-0000-0000-000000000007", "roles:assign", "Assign and change member roles"),
                permission("11111111-0000-0000-0000-000000000008", "applications:manage", "Register and manage client applications and credentials"),
                permission("11111111-0000-0000-0000-000000000009", "audit:read", "View tenant-scoped audit events and verify audit chain"),
                permission("11111111-0000-0000-0000-000000000010", "security:manage", "Configure organization security settings and passkey policies"),
                permission("11111111-0000-0000-0000-000000000011", "platform:admin", "Platform-level operational administration and platform health")
        );

        Map<String, Permission> byName = new LinkedHashMap<>();
        for (Permission definition : definitions) {
            Permission saved = permissionRepository.findByName(definition.getName())
                    .orElseGet(() -> permissionRepository.save(definition));
            byName.put(saved.getName(), saved);
        }
        return byName;
    }

    private void seedRoles(Map<String, Permission> permissions) {
        createRole("22222222-0000-0000-0000-000000000001", "PLATFORM_ADMIN",
                "Platform Administrator with system-level monitoring and configuration",
                permissions, Set.of("platform:admin"));
        createRole("22222222-0000-0000-0000-000000000002", "ORG_OWNER",
                "Organization Owner with complete administrative and ownership authority",
                permissions, Set.of(
                        "organization:read", "organization:update", "organization:delete",
                        "members:read", "members:invite", "members:remove", "roles:assign",
                        "applications:manage", "audit:read", "security:manage"));
        createRole("22222222-0000-0000-0000-000000000003", "ORG_ADMIN",
                "Organization Administrator managing permitted members and settings",
                permissions, Set.of(
                        "organization:read", "organization:update",
                        "members:read", "members:invite", "members:remove", "roles:assign",
                        "applications:manage", "audit:read"));
        createRole("22222222-0000-0000-0000-000000000004", "DEVELOPER",
                "Developer managing application registrations and API credentials",
                permissions, Set.of("organization:read", "applications:manage"));
        createRole("22222222-0000-0000-0000-000000000005", "MEMBER",
                "Standard Organization Member with access to permitted tenant resources",
                permissions, Set.of("organization:read"));
    }

    private void createRole(String id, String name, String description,
                            Map<String, Permission> permissions, Set<String> permissionNames) {
        if (roleRepository.findByName(name).isPresent()) {
            return;
        }
        Role role = new Role(UUID.fromString(id), name, description, true);
        role.setPermissions(permissionNames.stream().map(permissions::get).collect(Collectors.toSet()));
        roleRepository.save(role);
    }

    private static Permission permission(String id, String name, String description) {
        return new Permission(UUID.fromString(id), name, description);
    }
}
