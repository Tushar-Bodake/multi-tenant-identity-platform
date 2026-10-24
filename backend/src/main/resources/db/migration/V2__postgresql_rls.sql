-- ==============================================================================
-- SecureGate V2: PostgreSQL Row-Level Security (RLS) Policies
-- Enforces defense-in-depth tenant isolation at the database kernel level
-- ==============================================================================

-- 1. Enable and FORCE Row-Level Security on tenant-owned tables
ALTER TABLE organizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE organizations FORCE ROW LEVEL SECURITY;

ALTER TABLE organization_memberships ENABLE ROW LEVEL SECURITY;
ALTER TABLE organization_memberships FORCE ROW LEVEL SECURITY;

ALTER TABLE invitations ENABLE ROW LEVEL SECURITY;
ALTER TABLE invitations FORCE ROW LEVEL SECURITY;

ALTER TABLE applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE applications FORCE ROW LEVEL SECURITY;

ALTER TABLE audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_events FORCE ROW LEVEL SECURITY;

-- Helper condition logic:
-- A row is accessible if:
-- 1. app.is_platform_admin session setting is 'true', OR
-- 2. app.current_tenant_id matches the row's tenant id (cast to uuid)

-- ------------------------------------------------------------------------------
-- Policies for: organizations
-- ------------------------------------------------------------------------------
CREATE POLICY tenant_isolation_organizations_select ON organizations
    FOR SELECT
    USING (
        current_setting('app.is_platform_admin', true) = 'true'
        OR id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    );

CREATE POLICY tenant_isolation_organizations_insert ON organizations
    FOR INSERT
    WITH CHECK (
        current_setting('app.is_platform_admin', true) = 'true'
        OR id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
        OR NULLIF(current_setting('app.current_tenant_id', true), '') IS NULL
    );

CREATE POLICY tenant_isolation_organizations_update ON organizations
    FOR UPDATE
    USING (
        current_setting('app.is_platform_admin', true) = 'true'
        OR id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    );

CREATE POLICY tenant_isolation_organizations_delete ON organizations
    FOR DELETE
    USING (
        current_setting('app.is_platform_admin', true) = 'true'
        OR id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    );

-- ------------------------------------------------------------------------------
-- Policies for: organization_memberships
-- ------------------------------------------------------------------------------
CREATE POLICY tenant_isolation_memberships_select ON organization_memberships
    FOR SELECT
    USING (
        current_setting('app.is_platform_admin', true) = 'true'
        OR organization_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
        -- Also allow a user to see their own memberships to list organizations they belong to
        OR user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid
    );

CREATE POLICY tenant_isolation_memberships_insert ON organization_memberships
    FOR INSERT
    WITH CHECK (
        current_setting('app.is_platform_admin', true) = 'true'
        OR organization_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    );

CREATE POLICY tenant_isolation_memberships_update ON organization_memberships
    FOR UPDATE
    USING (
        current_setting('app.is_platform_admin', true) = 'true'
        OR organization_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    );

CREATE POLICY tenant_isolation_memberships_delete ON organization_memberships
    FOR DELETE
    USING (
        current_setting('app.is_platform_admin', true) = 'true'
        OR organization_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    );

-- ------------------------------------------------------------------------------
-- Policies for: invitations
-- ------------------------------------------------------------------------------
CREATE POLICY tenant_isolation_invitations_select ON invitations
    FOR SELECT
    USING (
        current_setting('app.is_platform_admin', true) = 'true'
        OR organization_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
        OR email = current_setting('app.current_user_email', true)
    );

CREATE POLICY tenant_isolation_invitations_insert ON invitations
    FOR INSERT
    WITH CHECK (
        current_setting('app.is_platform_admin', true) = 'true'
        OR organization_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    );

CREATE POLICY tenant_isolation_invitations_update ON invitations
    FOR UPDATE
    USING (
        current_setting('app.is_platform_admin', true) = 'true'
        OR organization_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    );

CREATE POLICY tenant_isolation_invitations_delete ON invitations
    FOR DELETE
    USING (
        current_setting('app.is_platform_admin', true) = 'true'
        OR organization_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    );

-- ------------------------------------------------------------------------------
-- Policies for: applications
-- ------------------------------------------------------------------------------
CREATE POLICY tenant_isolation_applications_all ON applications
    FOR ALL
    USING (
        current_setting('app.is_platform_admin', true) = 'true'
        OR organization_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    );

-- ------------------------------------------------------------------------------
-- Policies for: audit_events (Append-only)
-- ------------------------------------------------------------------------------
CREATE POLICY tenant_isolation_audit_select ON audit_events
    FOR SELECT
    USING (
        current_setting('app.is_platform_admin', true) = 'true'
        OR tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    );

CREATE POLICY tenant_isolation_audit_insert ON audit_events
    FOR INSERT
    WITH CHECK (
        current_setting('app.is_platform_admin', true) = 'true'
        OR tenant_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
        OR tenant_id IS NULL
    );

-- Revoke UPDATE and DELETE on audit_events to guarantee tamper-evident append-only invariant
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'securegate_app') THEN
        REVOKE UPDATE, DELETE ON audit_events FROM securegate_app;
    END IF;
END
$$;
