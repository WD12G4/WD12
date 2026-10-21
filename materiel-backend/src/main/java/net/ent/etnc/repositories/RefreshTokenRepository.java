package net.ent.etnc.repositories;

import jakarta.persistence.LockModeType;
import net.ent.etnc.models.RefreshToken;
import net.ent.etnc.repositories.commons.BaseRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Persistence access for refresh token hashes.
 */
@Repository
public interface RefreshTokenRepository extends BaseRepository<RefreshToken> {
    /**
     * Finds a refresh token by its SHA-256 hash.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Finds and locks a refresh token for transactional rotation.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshToken token where token.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    /**
     * Revokes every still-active token in the same family.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update RefreshToken token
               set token.revokedAt = :revokedAt
             where token.familyId = :familyId
               and token.revokedAt is null
            """)
    int revokeActiveByFamilyId(@Param("familyId") String familyId, @Param("revokedAt") Instant revokedAt);
}
