package com.securegate.audit.service;

import com.securegate.audit.domain.AuditEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditChainVerifierTest {

    @Mock private AuditService auditService;
    private AuditChainVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new AuditChainVerifier(auditService);
    }

    @Test
    @DisplayName("Should successfully verify valid audit chain")
    void testValidChain() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        List<AuditEvent> chain = new ArrayList<>();

        // Event 1 (Genesis)
        Instant t1 = Instant.ofEpochMilli(1000000);
        String h1 = AuditService.computeCanonicalHash(AuditService.GENESIS_PREV_HASH, t1, actorId, tenantId,
                "USER_REGISTER", "USER", actorId.toString(), "SUCCESS", "corr-1", "{}");
        AuditEvent e1 = new AuditEvent(actorId, tenantId, "USER_REGISTER", "USER", actorId.toString(), "SUCCESS", "corr-1", "{}", AuditService.GENESIS_PREV_HASH, h1, t1);

        // Event 2
        Instant t2 = Instant.ofEpochMilli(2000000);
        String h2 = AuditService.computeCanonicalHash(h1, t2, actorId, tenantId,
                "USER_LOGIN", "USER", actorId.toString(), "SUCCESS", "corr-2", "{}");
        AuditEvent e2 = new AuditEvent(actorId, tenantId, "USER_LOGIN", "USER", actorId.toString(), "SUCCESS", "corr-2", "{}", h1, h2, t2);

        chain.add(e1);
        chain.add(e2);

        when(auditService.getAuditChain(tenantId)).thenReturn(chain);

        AuditChainVerifier.VerificationResult result = verifier.verifyTenantChain(tenantId);
        assertThat(result.valid()).isTrue();
        assertThat(result.eventsVerified()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should detect modified data within audit event (hash tampering)")
    void testTamperedEventData() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        List<AuditEvent> chain = new ArrayList<>();

        Instant t1 = Instant.ofEpochMilli(1000000);
        String h1 = AuditService.computeCanonicalHash(AuditService.GENESIS_PREV_HASH, t1, actorId, tenantId,
                "USER_REGISTER", "USER", actorId.toString(), "SUCCESS", "corr-1", "{}");
        AuditEvent e1 = new AuditEvent(actorId, tenantId, "USER_REGISTER", "USER", actorId.toString(), "SUCCESS", "corr-1", "{}", AuditService.GENESIS_PREV_HASH, h1, t1);

        // Event 2 with forged hash or modified action
        Instant t2 = Instant.ofEpochMilli(2000000);
        AuditEvent e2 = new AuditEvent(actorId, tenantId, "TAMPERED_ACTION", "USER", actorId.toString(), "SUCCESS", "corr-2", "{}", h1, "fake_hash_12345", t2);

        chain.add(e1);
        chain.add(e2);

        when(auditService.getAuditChain(tenantId)).thenReturn(chain);

        AuditChainVerifier.VerificationResult result = verifier.verifyTenantChain(tenantId);
        assertThat(result.valid()).isFalse();
        assertThat(result.brokenIndex()).isEqualTo(1);
        assertThat(result.errorMessage()).contains("Hash mismatch");
    }

    @Test
    @DisplayName("Should detect broken link when previous hash does not match")
    void testBrokenLinkInChain() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        List<AuditEvent> chain = new ArrayList<>();

        Instant t1 = Instant.ofEpochMilli(1000000);
        String h1 = AuditService.computeCanonicalHash(AuditService.GENESIS_PREV_HASH, t1, actorId, tenantId,
                "USER_REGISTER", "USER", actorId.toString(), "SUCCESS", "corr-1", "{}");
        AuditEvent e1 = new AuditEvent(actorId, tenantId, "USER_REGISTER", "USER", actorId.toString(), "SUCCESS", "corr-1", "{}", AuditService.GENESIS_PREV_HASH, h1, t1);

        // Event 2 has incorrect prevHash
        Instant t2 = Instant.ofEpochMilli(2000000);
        AuditEvent e2 = new AuditEvent(actorId, tenantId, "LOGIN", "USER", actorId.toString(), "SUCCESS", "corr-2", "{}", "corrupted_prev_hash", "h2", t2);

        chain.add(e1);
        chain.add(e2);

        when(auditService.getAuditChain(tenantId)).thenReturn(chain);

        AuditChainVerifier.VerificationResult result = verifier.verifyTenantChain(tenantId);
        assertThat(result.valid()).isFalse();
        assertThat(result.brokenIndex()).isEqualTo(1);
        assertThat(result.errorMessage()).contains("Chain break");
    }
}
