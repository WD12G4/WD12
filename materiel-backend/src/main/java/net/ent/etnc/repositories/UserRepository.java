package net.ent.etnc.repositories;

import net.ent.etnc.models.User;
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
    Optional<User> findByUsername(String username);

    /**
     * Returns whether a username is already used.
     */
    boolean existsByUsername(String username);
}
