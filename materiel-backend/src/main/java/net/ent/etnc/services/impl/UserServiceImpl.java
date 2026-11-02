package net.ent.etnc.services.impl;

import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.models.User;
import net.ent.etnc.repositories.UserRepository;
import net.ent.etnc.services.UserService;
import net.ent.etnc.services.commons.AbstractService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import net.ent.etnc.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Transactional service for account business rules and actor lookup.
 */
@Slf4j
@Service
public class UserServiceImpl extends AbstractService<User, UserRepository> implements UserService {

    private final PasswordEncoder passwordEncoder;

    /**
     * Creates the account service.
     */
    @Autowired
    public UserServiceImpl(UserRepository repository, PasswordEncoder passwordEncoder) {
        super(repository);
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a user after enforcing username uniqueness and encoding the password.
     */
    @Override
    @Transactional
    public User create(User entity) {
        log.debug("Demande de création d'un utilisateur [username={}]", entity.getUsername());

        if (entity.getUsername() == null || entity.getUsername().isBlank()) {
            log.warn("Échec création utilisateur : le nom d'utilisateur est vide.");
            throw new ServiceException("Le nom d'utilisateur est obligatoire.");
        }
        if (repository.existsByUsername(entity.getUsername())) {
            log.warn("Échec création utilisateur : le nom d'utilisateur '{}' est déjà utilisé.", entity.getUsername());
            throw new ServiceException("Le nom d'utilisateur est déjà utilisé.");
        }
        if (entity.getPassword() == null || entity.getPassword().isBlank()) {
            log.warn("Échec création utilisateur : mot de passe manquant pour '{}'.", entity.getUsername());
            throw new ServiceException("Le mot de passe est obligatoire.");
        }

        entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        User created = super.create(entity);
        log.info("Utilisateur créé avec succès [id={}, username={}, role={}]",
                created.getId(), created.getUsername(), created.getRole());
        return created;
    }

    /**
     * Updates an existing user, re-encoding the password if a plain text password was set.
     */
    @Override
    @Transactional
    public User update(User entity) {
        log.debug("Demande de mise à jour de l'utilisateur [id={}, username={}]", entity.getId(), entity.getUsername());
        if (entity.getId() == null || !repository.existsById(entity.getId())) {
            log.warn("Échec mise à jour utilisateur : id introuvable ({})", entity.getId());
            throw new ResourceNotFoundException("Utilisateur introuvable avec l'id : " + entity.getId());
        }

        if (entity.getPassword() != null && !entity.getPassword().isBlank()) {
            boolean isAlreadyBCrypt = (entity.getPassword().startsWith("$2a$")
                    || entity.getPassword().startsWith("$2b$")
                    || entity.getPassword().startsWith("$2y$"))
                    && entity.getPassword().length() == 60;
            if (!isAlreadyBCrypt) {
                entity.setPassword(passwordEncoder.encode(entity.getPassword()));
            }
        }

        User updated = super.update(entity);
        log.info("Utilisateur mis à jour avec succès [id={}, username={}, role={}, enabled={}]",
                updated.getId(), updated.getUsername(), updated.getRole(), updated.isEnabled());
        return updated;
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
     * Returns the authenticated user reloaded from the repository within the current transaction.
     */
    @Override
    @Transactional(readOnly = true)
    public User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            log.warn("Tentative d'accès non authentifié à currentUser()");
            throw new AccessDeniedException("Utilisateur non authentifié");
        }

        String username = switch (authentication.getPrincipal()) {
            case User user -> user.getUsername();
            case UserDetails userDetails -> userDetails.getUsername();
            case String principalName -> principalName;
            default -> {
                log.warn("Principal d'authentification invalide rencontré : {}", authentication.getPrincipal().getClass());
                throw new AccessDeniedException("Type de principal non supporté");
            }
        };

        return repository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Utilisateur authentifié introuvable en base : {}", username);
                    return new AccessDeniedException("Utilisateur introuvable : " + username);
                });
    }
}
