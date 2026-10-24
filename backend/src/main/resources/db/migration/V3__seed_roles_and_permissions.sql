-- ==============================================================================
-- SecureGate V3: Seed Roles and Permissions
-- Pre-populates system roles and RBAC mappings
-- ==============================================================================

-- 1. Insert System Permissions
INSERT INTO permissions (id, name, description) VALUES
    ('11111111-0000-0000-0000-000000000001', 'organization:read', 'View organization details and configuration'),
    ('11111111-0000-0000-0000-000000000002', 'organization:update', 'Update organization settings and authentication policy'),
    ('11111111-0000-0000-0000-000000000003', 'organization:delete', 'Delete organization and transfer ownership'),
    ('11111111-0000-0000-0000-000000000004', 'members:read', 'View organization members and invitations'),
    ('11111111-0000-0000-0000-000000000005', 'members:invite', 'Invite new members to the organization'),
    ('11111111-0000-0000-0000-000000000006', 'members:remove', 'Remove members and revoke pending invitations'),
    ('11111111-0000-0000-0000-000000000007', 'roles:assign', 'Assign and change member roles'),
    ('11111111-0000-0000-0000-000000000008', 'applications:manage', 'Register and manage client applications and credentials'),
    ('11111111-0000-0000-0000-000000000009', 'audit:read', 'View tenant-scoped audit events and verify audit chain'),
    ('11111111-0000-0000-0000-000000000010', 'security:manage', 'Configure organization security settings and passkey policies'),
    ('11111111-0000-0000-0000-000000000011', 'platform:admin', 'Platform-level operational administration and platform health')
ON CONFLICT (id) DO NOTHING;

-- 2. Insert System Roles
INSERT INTO roles (id, name, description, is_system_role) VALUES
    ('22222222-0000-0000-0000-000000000001', 'PLATFORM_ADMIN', 'Platform Administrator with system-level monitoring and configuration', TRUE),
    ('22222222-0000-0000-0000-000000000002', 'ORG_OWNER', 'Organization Owner with complete administrative and ownership authority', TRUE),
    ('22222222-0000-0000-0000-000000000003', 'ORG_ADMIN', 'Organization Administrator managing permitted members and settings', TRUE),
    ('22222222-0000-0000-0000-000000000004', 'DEVELOPER', 'Developer managing application registrations and API credentials', TRUE),
    ('22222222-0000-0000-0000-000000000005', 'MEMBER', 'Standard Organization Member with access to permitted tenant resources', TRUE)
ON CONFLICT (id) DO NOTHING;

-- 3. Map Permissions to Roles

-- ORG_OWNER has all organization-level permissions
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('22222222-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000001'),
    ('22222222-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000002'),
    ('22222222-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000003'),
    ('22222222-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000004'),
    ('22222222-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000005'),
    ('22222222-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000006'),
    ('22222222-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000007'),
    ('22222222-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000008'),
    ('22222222-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000009'),
    ('22222222-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000010')
ON CONFLICT DO NOTHING;

-- ORG_ADMIN has read/update, member management (cannot delete org or assign owner)
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('22222222-0000-0000-0000-000000000003', '11111111-0000-0000-0000-000000000001'),
    ('22222222-0000-0000-0000-000000000003', '11111111-0000-0000-0000-000000000002'),
    ('22222222-0000-0000-0000-000000000003', '11111111-0000-0000-0000-000000000004'),
    ('22222222-0000-0000-0000-000000000003', '11111111-0000-0000-0000-000000000005'),
    ('22222222-0000-0000-0000-000000000003', '11111111-0000-0000-0000-000000000006'),
    ('22222222-0000-0000-0000-000000000003', '11111111-0000-0000-0000-000000000007'),
    ('22222222-0000-0000-0000-000000000003', '11111111-0000-0000-0000-000000000008'),
    ('22222222-0000-0000-0000-000000000003', '11111111-0000-0000-0000-000000000009')
ON CONFLICT DO NOTHING;

-- DEVELOPER has application management and read permissions
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('22222222-0000-0000-0000-000000000004', '11111111-0000-0000-0000-000000000001'),
    ('22222222-0000-0000-0000-000000000004', '11111111-0000-0000-0000-000000000008')
ON CONFLICT DO NOTHING;

-- MEMBER has basic read permission
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('22222222-0000-0000-0000-000000000005', '11111111-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- PLATFORM_ADMIN has platform:admin permission
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('22222222-0000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000011')
ON CONFLICT DO NOTHING;
