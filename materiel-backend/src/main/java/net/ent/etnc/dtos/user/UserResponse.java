package net.ent.etnc.dtos.user;

import net.ent.etnc.models.enums.ERole;

/**
 * Données d'un utilisateur renvoyées par l'API (sans mot de passe).
 */
public record UserResponse(
        Long id,
        Long version,
        String username,
        ERole role,
        boolean enabled
) {}
