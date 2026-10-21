package net.ent.etnc.models;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.ent.etnc.models.commons.AbstractPersistableWithIdSetter;

import java.time.Instant;

/**
 * Persisted refresh token metadata storing only a SHA-256 token hash.
 */
@Entity
@Table(name = "refresh_tokens", uniqueConstraints = @UniqueConstraint(
        name = "uk__refresh_tokens__token_hash", columnNames = "token_hash"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends AbstractPersistableWithIdSetter<Long> {
    /**
     * SHA-256 hexadecimal hash of the raw refresh token.
     */
    @Getter
    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    private String tokenHash;

    /**
     * Opaque family id shared by successive rotations.
     */
    @Getter
    @Column(name = "family_id", nullable = false, length = 36, updatable = false)
    private String familyId;

    /**
     * Account owning this token family.
     */
    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk__refresh_tokens__users"))
    private User user;

    /**
     * Creation timestamp.
     */
    @Getter
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Expiration timestamp.
     */
    @Getter
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    /**
     * Revocation timestamp, null while active.
     */
    @Getter
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Hash of the replacement token created during rotation.
     */
    @Getter
    @Column(name = "replaced_by_token_hash", length = 64)
    private String replacedByTokenHash;

    /**
     * Audit IP captured at creation time.
     */
    @Getter
    @Column(name = "created_by_ip", length = 64, updatable = false)
    private String createdByIp;

    /**
     * Audit User-Agent captured at creation time.
     */
    @Getter
    @Column(name = "created_by_user_agent", length = 512, updatable = false)
    private String createdByUserAgent;

    /**
     * Creates a persisted refresh-token record from a precomputed hash.
     */
    public RefreshToken(String tokenHash, String familyId, User user, Instant createdAt,
                        Instant expiresAt, String createdByIp, String createdByUserAgent) {
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.user = user;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.createdByIp = createdByIp;
        this.createdByUserAgent = createdByUserAgent;
    }

    /**
     * Returns true when this token is expired at the provided instant.
     */
    public boolean isExpiredAt(Instant instant) {
        return !expiresAt.isAfter(instant);
    }

    /**
     * Returns true when this token has been revoked.
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Revokes the token once and records the replacement hash when known.
     */
    public void revoke(Instant instant, String replacementHash) {
        if (revokedAt == null) {
            revokedAt = instant;
            replacedByTokenHash = replacementHash;
        }
    }
}
