package net.ent.etnc.services.impl;

import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.models.User;
import net.ent.etnc.repositories.UserRepository;
import net.ent.etnc.services.UserService;
import net.ent.etnc.services.commons.AbstractService;
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
