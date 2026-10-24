package com.securegate.audit.service;

import com.securegate.audit.domain.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuditChainVerifier {

    private static final Logger log = LoggerFactory.getLogger(AuditChainVerifier.class);

    private final AuditService auditService;

    public record VerificationResult(
            boolean valid,
            int eventsVerified,
            UUID brokenEventId,
            Integer brokenIndex,
            String errorMessage
    ) {
        public static VerificationResult success(int count) {
            return new VerificationResult(true, count, null, null, "Audit chain integrity verified successfully");
        }

        public static VerificationResult failure(int index, UUID eventId, String message) {
            return new VerificationResult(false, index, eventId, index, message);
        }
    }

    public AuditChainVerifier(AuditService auditService) {
        this.auditService = auditService;
    }

    public VerificationResult verifyTenantChain(UUID tenantId) {
        List<AuditEvent> chain = auditService.getAuditChain(tenantId);
        if (chain.isEmpty()) {
            return VerificationResult.success(0);
        }

        String expectedPrevHash = AuditService.GENESIS_PREV_HASH;

        for (int i = 0; i < chain.size(); i++) {
            AuditEvent event = chain.get(i);

            // 1. Verify previous hash pointer
            if (!expectedPrevHash.equals(event.getPrevHash())) {
                String error = String.format("Chain break at index %d: expected prevHash '%s' but found '%s'",
                        i, expectedPrevHash, event.getPrevHash());
                log.error("AUDIT CHAIN INTEGRITY COMPROMISED: {}", error);
                return VerificationResult.failure(i, event.getId(), error);
            }

            // 2. Recompute and verify current hash
            String recomputedHash = AuditService.computeCanonicalHash(
                    event.getPrevHash(),
                    event.getTimestamp(),
                    event.getActorId(),
                    event.getTenantId(),
                    event.getAction(),
                    event.getTargetType(),
                    event.getTargetId(),
                    event.getOutcome(),
                    event.getCorrelationId(),
                    event.getMetadataJson()
            );

            if (!recomputedHash.equals(event.getCurrentHash())) {
                String error = String.format("Hash mismatch at index %d (id: %s): expected '%s' but event has '%s'. Data was modified!",
                        i, event.getId(), recomputedHash, event.getCurrentHash());
                log.error("AUDIT CHAIN INTEGRITY COMPROMISED: {}", error);
                return VerificationResult.failure(i, event.getId(), error);
            }

            expectedPrevHash = event.getCurrentHash();
        }

        log.info("Successfully verified audit chain for tenantId='{}' ({} events verified)", tenantId, chain.size());
        return VerificationResult.success(chain.size());
    }
}
