package com.securegate.identity.dto;

import com.securegate.tenant.dto.OrganizationDto;
import java.util.List;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    UserDto user,
    OrganizationDto activeOrganization,
    List<OrganizationDto> organizations
) {
    public static AuthResponse of(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserDto user,
        OrganizationDto activeOrganization,
        List<OrganizationDto> organizations
    ) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user, activeOrganization, organizations);
    }
}
