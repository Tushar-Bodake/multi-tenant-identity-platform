package com.securegate.identity.dto;

import com.securegate.identity.domain.UserStatus;
import java.util.UUID;

public record UserDto(
    UUID id,
    String fullName,
    String email,
    boolean emailVerified,
    UserStatus status
) {}
