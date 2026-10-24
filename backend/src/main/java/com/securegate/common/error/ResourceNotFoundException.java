package com.securegate.common.error;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends SecureGateException {
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
