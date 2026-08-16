package app.pastehub.api.transfer;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transfers")
class Transfer {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "pickup_code", nullable = false, unique = true, length = 6)
    private String pickupCode;

    @Column(nullable = false, columnDefinition = "CLOB")
    private String content;

    @Column(name = "delete_token_hash", nullable = false, length = 64)
    private String deleteTokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Transfer() {
    }

    Transfer(String id, String pickupCode, String content, String deleteTokenHash, Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.pickupCode = pickupCode;
        this.content = content;
        this.deleteTokenHash = deleteTokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    String id() { return id; }
    String pickupCode() { return pickupCode; }
    String content() { return content; }
    String deleteTokenHash() { return deleteTokenHash; }
    Instant expiresAt() { return expiresAt; }
    Instant deletedAt() { return deletedAt; }

    boolean isExpired(Instant now) { return !expiresAt.isAfter(now); }
    boolean isDeleted() { return deletedAt != null; }
    void markDeleted(Instant now) { if (deletedAt == null) deletedAt = now; }
}
