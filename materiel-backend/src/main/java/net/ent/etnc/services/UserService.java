package net.ent.etnc.services;

import net.ent.etnc.models.User;
import net.ent.etnc.services.commons.Service;

import java.util.Optional;

/**
 * Business contract for application accounts.
 */
public interface UserService extends Service<User, Long> {
    /**
     * Finds an account by login username.
     */
    Optional<User> findByUsername(String username);

    /**
     * Returns the authenticated user from the Spring Security context.
     */
    User currentUser();
}
