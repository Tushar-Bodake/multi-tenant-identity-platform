package com.securegate.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "passkey_credentials")
public class PasskeyCredential {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "credential_id", nullable = false, unique = true, length = 500)
    private String credentialId;

    @Column(name = "public_key_cose", nullable = false)
    private byte[] publicKeyCose;

    @Column(name = "sign_count", nullable = false)
    private long signCount = 0;

    @Column(name = "aaguid", length = 64)
    private String aaguid;

    @Column(name = "nickname", nullable = false, length = 100)
    private String nickname;

    @Column(name = "transports", length = 255)
    private String transports;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    public PasskeyCredential() {
    }

    public PasskeyCredential(UUID userId, String credentialId, byte[] publicKeyCose, long signCount, String aaguid, String nickname, String transports) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.credentialId = credentialId;
        this.publicKeyCose = publicKeyCose;
        this.signCount = signCount;
        this.aaguid = aaguid;
        this.nickname = nickname;
        this.transports = transports;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public byte[] getPublicKeyCose() {
        return publicKeyCose;
    }

    public long getSignCount() {
        return signCount;
    }

    public void setSignCount(long signCount) {
        this.signCount = signCount;
    }

    public String getAaguid() {
        return aaguid;
    }

    public String getNickname() {
        return nickname;
    }

    public String getTransports() {
        return transports;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
