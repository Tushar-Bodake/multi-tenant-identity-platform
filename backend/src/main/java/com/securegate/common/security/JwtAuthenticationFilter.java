package com.securegate.common.security;

import com.nimbusds.jwt.JWTClaimsSet;
import com.securegate.authentication.service.JwtService;
import com.securegate.tenant.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    JWTClaimsSet claims = jwtService.validateAndParseToken(token);
                    String userId = claims.getSubject();
                    String email = (String) claims.getClaim("email");
                    String role = (String) claims.getClaim("role");
                    String tenantIdStr = (String) claims.getClaim("tenant_id");

                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    if (role != null) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                    }

                    List<?> permissions = (List<?>) claims.getClaim("permissions");
                    if (permissions != null) {
                        for (Object p : permissions) {
                            authorities.add(new SimpleGrantedAuthority(p.toString()));
                        }
                    }

                    if (userId != null) {
                        TenantContext.setUserId(UUID.fromString(userId));
                    }
                    if (email != null) {
                        TenantContext.setUserEmail(email);
                    }
                    if (tenantIdStr != null) {
                        TenantContext.setTenantId(UUID.fromString(tenantIdStr));
                    }

                    if ("PLATFORM_ADMIN".equals(role)) {
                        TenantContext.setPlatformAdmin(true);
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                } catch (Exception e) {
                    // Invalid token: SecurityContext remains unauthenticated
                    logger.debug("JWT validation failed: " + e.getMessage());
                }
            }

            filterChain.doFilter(request, response);

        } finally {
            // Guarantee TenantContext cleanup when thread returns to pool
            TenantContext.clear();
        }
    }
}
