package net.ent.etnc.dtos.user;

import net.ent.etnc.models.enums.ERole;

/**
 * Données d'entrée pour la mise à jour d'un utilisateur.
 */
public record UserUpdateRequest(
        ERole role,

        Boolean enabled
) {}
