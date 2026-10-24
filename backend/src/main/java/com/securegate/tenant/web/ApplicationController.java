package com.securegate.tenant.web;

import com.securegate.audit.service.AuditService;
import com.securegate.common.SecurityUtils;
import com.securegate.common.error.ResourceNotFoundException;
import com.securegate.tenant.domain.Application;
import com.securegate.tenant.repository.ApplicationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/applications")
@Tag(name = "Applications", description = "Tenant-scoped client applications and credentials")
public class ApplicationController {

    private final ApplicationRepository applicationRepository;
    private final AuditService auditService;

    public record CreateAppRequest(
            @NotBlank String name,
            String redirectUris
    ) {}

    public record ApplicationCreatedDto(
            UUID id,
            String name,
            String clientId,
            String clientSecret, // Returned ONLY once upon creation!
            String redirectUris
    ) {}

    public ApplicationController(ApplicationRepository applicationRepository, AuditService auditService) {
        this.applicationRepository = applicationRepository;
        this.auditService = auditService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission(authentication, #organizationId, 'applications:manage')")
    @Operation(summary = "Register a new tenant application and generate credentials")
    public ResponseEntity<ApplicationCreatedDto> createApplication(
            @PathVariable UUID organizationId,
            @Valid @RequestBody CreateAppRequest request,
            Authentication authentication) {

        UUID actorId = UUID.fromString(authentication.getPrincipal().toString());

        String clientId = "sg_client_" + SecurityUtils.generateSecureToken(16);
        String rawSecret = "sg_secret_" + SecurityUtils.generateSecureToken(32);
        String secretHash = SecurityUtils.sha256Hex(rawSecret);

        Application app = new Application(organizationId, request.name(), clientId, secretHash, request.redirectUris());
        app = applicationRepository.save(app);

        auditService.logEvent(actorId, organizationId, "APPLICATION_CREATE", "APPLICATION",
                app.getId().toString(), "SUCCESS", UUID.randomUUID().toString(),
                "{\"name\":\"" + request.name() + "\",\"clientId\":\"" + clientId + "\"}");

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApplicationCreatedDto(app.getId(), app.getName(), app.getClientId(), rawSecret, app.getRedirectUris())
        );
    }

    @GetMapping
    @PreAuthorize("@authz.hasPermission(authentication, #organizationId, 'applications:manage')")
    @Operation(summary = "List registered applications for the organization")
    public ResponseEntity<List<Application>> listApplications(@PathVariable UUID organizationId) {
        List<Application> apps = applicationRepository.findByOrganizationId(organizationId);
        // Do not leak secret hashes in GET lists
        apps.forEach(a -> a.setClientSecretHash("[PROTECTED]"));
        return ResponseEntity.ok(apps);
    }

    @DeleteMapping("/{appId}")
    @PreAuthorize("@authz.hasPermission(authentication, #organizationId, 'applications:manage')")
    @Operation(summary = "Delete an application registration")
    public ResponseEntity<Map<String, String>> deleteApplication(
            @PathVariable UUID organizationId,
            @PathVariable UUID appId,
            Authentication authentication) {

        UUID actorId = UUID.fromString(authentication.getPrincipal().toString());
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!app.getOrganizationId().equals(organizationId)) {
            throw new ResourceNotFoundException("Application not found in this organization");
        }

        applicationRepository.delete(app);

        auditService.logEvent(actorId, organizationId, "APPLICATION_DELETE", "APPLICATION",
                appId.toString(), "SUCCESS", UUID.randomUUID().toString(), "{}");

        return ResponseEntity.ok(Map.of("message", "Application deleted successfully"));
    }
}
