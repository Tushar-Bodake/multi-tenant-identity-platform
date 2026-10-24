package com.securegate.audit.web;

import com.securegate.audit.domain.AuditEvent;
import com.securegate.audit.service.AuditChainVerifier;
import com.securegate.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/audit-logs")
@Tag(name = "Audit Logs", description = "Tamper-evident audit logging and chain integrity verification")
public class AuditController {

    private final AuditService auditService;
    private final AuditChainVerifier auditChainVerifier;

    public AuditController(AuditService auditService, AuditChainVerifier auditChainVerifier) {
        this.auditService = auditService;
        this.auditChainVerifier = auditChainVerifier;
    }

    @GetMapping
    @PreAuthorize("@authz.hasPermission(authentication, #organizationId, 'audit:read')")
    @Operation(summary = "Get paginated audit events for this organization")
    public ResponseEntity<Page<AuditEvent>> getAuditLogs(
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "timestamp"));
        return ResponseEntity.ok(auditService.getTenantAuditLogs(organizationId, pageRequest));
    }

    @GetMapping("/verify")
    @PreAuthorize("@authz.hasPermission(authentication, #organizationId, 'audit:read')")
    @Operation(summary = "Verify the SHA-256 cryptographic chain integrity of this organization's audit log")
    public ResponseEntity<AuditChainVerifier.VerificationResult> verifyAuditChain(@PathVariable UUID organizationId) {
        AuditChainVerifier.VerificationResult result = auditChainVerifier.verifyTenantChain(organizationId);
        return ResponseEntity.ok(result);
    }
}
