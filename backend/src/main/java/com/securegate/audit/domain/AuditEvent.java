package com.securegate.audit.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "target_type", nullable = false, length = 100)
    private String targetType;

    @Column(name = "target_id")
    private String targetId;

    @Column(name = "outcome", nullable = false, length = 50)
    private String outcome;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "prev_hash", nullable = false, length = 64)
    private String prevHash;

    @Column(name = "current_hash", nullable = false, length = 64)
    private String currentHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AuditEvent() {
    }

    public AuditEvent(UUID actorId, UUID tenantId, String action, String targetType, String targetId,
                      String outcome, String correlationId, String metadataJson, String prevHash, String currentHash) {
        this(actorId, tenantId, action, targetType, targetId, outcome, correlationId, metadataJson, prevHash, currentHash, Instant.now());
    }

    public AuditEvent(UUID actorId, UUID tenantId, String action, String targetType, String targetId,
                      String outcome, String correlationId, String metadataJson, String prevHash, String currentHash, Instant timestamp) {
        this.id = UUID.randomUUID();
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.actorId = actorId;
        this.tenantId = tenantId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.outcome = outcome;
        this.correlationId = correlationId;
        this.metadataJson = metadataJson;
        this.prevHash = prevHash;
        this.currentHash = currentHash;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public UUID getActorId() {
        return actorId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
