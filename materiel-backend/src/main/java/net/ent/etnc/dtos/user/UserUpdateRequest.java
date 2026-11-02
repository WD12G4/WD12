package net.ent.etnc.dtos.user;

import jakarta.validation.constraints.Size;
import net.ent.etnc.models.enums.ERole;

/**
 * Données d'entrée pour la mise à jour d'un utilisateur.
 */
public record UserUpdateRequest(
        @Size(min = 6, max = 255, message = "Le mot de passe doit comporter au moins 6 caractères")
        String password,

        ERole role,

        Boolean enabled
) {}
