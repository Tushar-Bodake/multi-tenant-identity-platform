package com.securegate.tenant.dto;

import java.util.UUID;

public record OrganizationDto(
    UUID id,
    String name,
    String slug,
    String status,
    String role
) {}
