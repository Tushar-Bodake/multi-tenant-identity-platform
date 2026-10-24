package com.securegate.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "external_identities", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"provider", "provider_subject"})
})
public class ExternalIdentity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_subject", nullable = false)
    private String providerSubject;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;

    public ExternalIdentity() {
    }

    public ExternalIdentity(UUID userId, String provider, String providerSubject, String email) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.email = email;
        this.linkedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (linkedAt == null) {
            linkedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderSubject() {
        return providerSubject;
    }

    public String getEmail() {
        return email;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }
}
