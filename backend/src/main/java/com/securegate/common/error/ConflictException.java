package com.securegate.common.error;

import org.springframework.http.HttpStatus;

public class ConflictException extends SecureGateException {
    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
