package com.securegate.common.error;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends SecureGateException {
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
