package net.ent.etnc.repositories;

import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.ERole;
import net.ent.etnc.repositories.commons.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Persistence access for application users.
 */
@Repository
public interface UserRepository extends BaseRepository<User> {

    /**
     * Finds a user by login username.
     */
    Optional<User> findByUsername(final String username);

    /**
     * Returns whether a username is already used.
     */
    boolean existsByUsername(final String username);

    /**
     * Compte le nombre d'utilisateurs actifs ayant un rôle donné.
     */
    long countByRoleAndEnabledTrue(final ERole role);
}
