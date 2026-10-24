package com.securegate.audit.service;

import com.securegate.audit.domain.AuditEvent;
import com.securegate.audit.repository.AuditRepository;
import com.securegate.common.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    public static final String GENESIS_PREV_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public static String computeCanonicalHash(String prevHash, Instant timestamp, UUID actorId, UUID tenantId,
                                              String action, String targetType, String targetId,
                                              String outcome, String correlationId, String metadataJson) {
        String canonical = String.join("|",
                prevHash != null ? prevHash : GENESIS_PREV_HASH,
                timestamp != null ? String.valueOf(timestamp.toEpochMilli()) : "0",
                actorId != null ? actorId.toString() : "null",
                tenantId != null ? tenantId.toString() : "null",
                action != null ? action : "",
                targetType != null ? targetType : "",
                targetId != null ? targetId : "",
                outcome != null ? outcome : "",
                correlationId != null ? correlationId : "",
                metadataJson != null ? metadataJson : ""
        );
        return SecurityUtils.sha256Hex(canonical);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized AuditEvent logEvent(UUID actorId, UUID tenantId, String action, String targetType,
                               String targetId, String outcome, String correlationId, String metadataJson) {
        // Fetch latest event in tenant chain
        Optional<AuditEvent> latest = auditRepository.findLatestEventForTenant(tenantId);
        String prevHash = latest.map(AuditEvent::getCurrentHash).orElse(GENESIS_PREV_HASH);

        Instant timestamp = Instant.now();
        String currentHash = computeCanonicalHash(prevHash, timestamp, actorId, tenantId, action, targetType, targetId, outcome, correlationId, metadataJson);

        AuditEvent event = new AuditEvent(actorId, tenantId, action, targetType, targetId, outcome, correlationId, metadataJson, prevHash, currentHash);
        AuditEvent saved = auditRepository.save(event);
        log.info("Audit event logged: action='{}', outcome='{}', tenantId='{}', hash='{}'", action, outcome, tenantId, currentHash);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> getTenantAuditLogs(UUID tenantId, Pageable pageable) {
        return auditRepository.findByTenantIdPaged(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getAuditChain(UUID tenantId) {
        return auditRepository.findChainForTenant(tenantId);
    }
}
