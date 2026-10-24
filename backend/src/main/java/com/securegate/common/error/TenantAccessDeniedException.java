package com.securegate.common.error;

import org.springframework.http.HttpStatus;

public class TenantAccessDeniedException extends SecureGateException {
    public TenantAccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
