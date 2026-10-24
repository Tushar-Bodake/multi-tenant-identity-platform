package com.securegate.tenant.context;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> PLATFORM_ADMIN = ThreadLocal.withInitial(() -> false);

    private TenantContext() {
    }

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void setPlatformAdmin(boolean isPlatformAdmin) {
        PLATFORM_ADMIN.set(isPlatformAdmin);
    }

    public static boolean isPlatformAdmin() {
        return Boolean.TRUE.equals(PLATFORM_ADMIN.get());
    }

    public static void clear() {
        CURRENT_TENANT.remove();
        PLATFORM_ADMIN.remove();
    }
}
