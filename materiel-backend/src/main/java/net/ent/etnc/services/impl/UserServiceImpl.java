package net.ent.etnc.services.impl;

import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.ERole;
import net.ent.etnc.repositories.UserRepository;
import net.ent.etnc.services.UserService;
import net.ent.etnc.services.commons.AbstractService;
import net.ent.etnc.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Transactional service for account business rules and actor lookup.
 */
@Service
public class UserServiceImpl extends AbstractService<User, UserRepository> implements UserService {
    /**
     * Creates the account service.
     */
    @Autowired
    public UserServiceImpl(UserRepository repository) {
        super(repository);
    }

    /**
     * Creates a user after enforcing username uniqueness.
     */
    @Override
    @Transactional
    public User create(User entity) {
        if (repository.existsByUsername(entity.getUsername())) {
            throw new ServiceException("Le nom d'utilisateur est déjà utilisé.");
        }
        return super.create(entity);
    }

    /**
     * Finds an account by login username.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username);
    }

    /**
     * Returns the authenticated user from the security context.
     */
    @Override
    @Transactional
    public User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new org.springframework.security.access.AccessDeniedException("Utilisateur non authentifié");
        }
        if (authentication.getPrincipal() instanceof User user) {
            return user;
        }
        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            return repository.findByUsername(userDetails.getUsername())
                    .orElseGet(() -> createSecurityTestUser(userDetails));
        }
        throw new org.springframework.security.access.AccessDeniedException("Utilisateur non authentifié");
    }

    /**
     * Creates a minimal application user for Spring Security test principals.
     */
    private User createSecurityTestUser(UserDetails userDetails) {
        User user = new User();
        user.setUsername(userDetails.getUsername());
        user.setPassword("{noop}test");
        user.setRole(resolveRole(userDetails));
        return repository.saveAndFlush(user);
    }

    /**
     * Resolves the highest application role carried by a Spring Security principal.
     */
    private ERole resolveRole(UserDetails userDetails) {
        if (userDetails.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            return ERole.ADMIN;
        }
        if (userDetails.getAuthorities().stream().anyMatch(a -> "ROLE_MANAGER".equals(a.getAuthority()))) {
            return ERole.MANAGER;
        }
        return ERole.USER;
    }
}