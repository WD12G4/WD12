package net.ent.etnc.services.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.ERole;
import net.ent.etnc.repositories.UserRepository;
import net.ent.etnc.services.UserService;
import net.ent.etnc.services.commons.AbstractService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import net.ent.etnc.services.commons.ServiceException;

/**
 * Transactional service for account business rules and actor lookup.
 */
@Slf4j
@Service
public class UserServiceImpl extends AbstractService<User, UserRepository> implements UserService {

    /**
     * Creates the account service.
     */
    @Autowired
    public UserServiceImpl(final UserRepository repository) {
        super(repository);
    }

    /**
     * Creates a user after enforcing username uniqueness.
     */
    @Override
    @Transactional
    public User create(final User entity) {
        if (entity == null) {
            log.warn("Échec création utilisateur : l'entité est null.");
            throw new ServiceException("L'utilisateur ne peut pas être null.");
        }
        log.debug("Demande de création d'un utilisateur [username={}]", entity.getUsername());

        validateUsername(entity.getUsername());

        if (repository.existsByUsername(entity.getUsername().trim())) {
            log.warn("Échec création utilisateur : le nom d'utilisateur '{}' est déjà utilisé.", entity.getUsername());
            throw new ServiceException("Le nom d'utilisateur est déjà utilisé.");
        }

        User created = super.create(entity);
        log.info("Utilisateur créé avec succès [id={}, username={}, role={}]", created.getId(), created.getUsername(), created.getRole());
        return created;
    }

    /**
     * Updates an existing user.
     */
    @Override
    @Transactional
    public User update(final User entity) {
        if (entity == null) {
            log.warn("Échec mise à jour utilisateur : l'entité est null.");
            throw new ServiceException("L'utilisateur ne peut pas être null.");
        }
        log.debug("Demande de mise à jour de l'utilisateur [id={}, username={}]", entity.getId(), entity.getUsername());
        if (entity.getId() == null || !repository.existsById(entity.getId())) {
            log.warn("Échec mise à jour utilisateur : id introuvable ({})", entity.getId());
            throw new ResourceNotFoundException("Utilisateur introuvable avec l'id : " + entity.getId());
        }

        User existing = repository.findById(entity.getId()).orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'id : " + entity.getId()));

        validateUsername(entity.getUsername());

        repository.findByUsername(entity.getUsername().trim()).ifPresent(other -> {
            if (!other.getId().equals(entity.getId())) {
                log.warn("Échec mise à jour utilisateur : le nom d'utilisateur '{}' est déjà utilisé par l'id {}.", entity.getUsername(), other.getId());
                throw new ServiceException("Le nom d'utilisateur est déjà utilisé.");
            }
        });

        //Empêcher de désactiver ou rétrograder le dernier administrateur actif
        boolean wasActiveAdmin = existing.getRole() == ERole.ADMIN && existing.isEnabled();
        boolean remainsActiveAdmin = entity.getRole() == ERole.ADMIN && entity.isEnabled();
        if (wasActiveAdmin && !remainsActiveAdmin) {
            long activeAdminCount = repository.countByRoleAndEnabledTrue(ERole.ADMIN);
            if (activeAdminCount <= 1) {
                log.warn("Tentative de désactivation ou rétrogradation du dernier administrateur actif [id={}, username={}]", entity.getId(), entity.getUsername());
                throw new ServiceException("Impossible de désactiver ou rétrograder le dernier administrateur actif.");
            }
        }

        User updated = super.update(entity);
        log.info("Utilisateur mis à jour avec succès [id={}, username={}, role={}, enabled={}]", updated.getId(), updated.getUsername(), updated.getRole(), updated.isEnabled());
        return updated;
    }

    /**
     * Soft delete (archivage) d'un utilisateur conformément à RG-40.
     */
    @Override
    @Transactional
    public void deleteById(final Long id) {
        User user = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'id : " + id));

        //Empêcher de supprimer/désactiver le dernier administrateur actif
        if (user.getRole() == ERole.ADMIN && user.isEnabled()) {
            long activeAdminCount = repository.countByRoleAndEnabledTrue(ERole.ADMIN);
            if (activeAdminCount <= 1) {
                log.warn("Tentative de suppression/désactivation du dernier administrateur actif [id={}, username={}]", user.getId(), user.getUsername());
                throw new ServiceException("Impossible de supprimer ou désactiver le dernier administrateur actif.");
            }
        }

        user.setEnabled(false);
        repository.saveAndFlush(user);
        log.info("Utilisateur désactivé (archivé) avec succès [id={}, username={}]", user.getId(), user.getUsername());
    }

    /**
     * Finds an account by login username.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(final String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return repository.findByUsername(username.trim());
    }

    /**
     * Returns the authenticated user reloaded from the repository within the current transaction.
     */
    @Override
    @Transactional(readOnly = true)
    public User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            log.warn("Tentative d'accès non authentifié à currentUser()");
            throw new AccessDeniedException("Utilisateur non authentifié");
        }

        Object principal = authentication.getPrincipal();
        if (principal == null) {
            log.warn("Principal d'authentification null rencontré");
            throw new AccessDeniedException("Principal non renseigné");
        }

        String username = switch (principal) {
            case UserDetails userDetails -> userDetails.getUsername();
            case Jwt jwt -> {
                String preferred = jwt.getClaimAsString("preferred_username");
                yield (preferred != null && !preferred.isBlank()) ? preferred : jwt.getSubject();
            }
            case String principalName -> principalName;
            default -> {
                log.warn("Principal d'authentification invalide rencontré : {}", principal.getClass());
                throw new AccessDeniedException("Type de principal non supporté");
            }
        };

        return repository.findByUsername(username).orElseThrow(() -> {
            log.warn("Utilisateur authentifié introuvable en base : {}", username);
            return new AccessDeniedException("Utilisateur introuvable : " + username);
        });
    }

    private void validateUsername(final String username) {
        if (username == null || username.isBlank()) {
            log.warn("Validation échouée : le nom d'utilisateur est vide.");
            throw new ServiceException("Le nom d'utilisateur est obligatoire.");
        }
    }
}
