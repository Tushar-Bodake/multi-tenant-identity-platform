package com.securegate.tenant.context;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> PLATFORM_ADMIN = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<UUID> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USER_EMAIL = new ThreadLocal<>();

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

    public static void setUserId(UUID userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static UUID getUserId() {
        return CURRENT_USER_ID.get();
    }

    public static void setUserEmail(String email) {
        CURRENT_USER_EMAIL.set(email);
    }

    public static String getUserEmail() {
        return CURRENT_USER_EMAIL.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
        PLATFORM_ADMIN.remove();
        CURRENT_USER_ID.remove();
        CURRENT_USER_EMAIL.remove();
    }
}

