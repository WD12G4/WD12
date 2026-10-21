package net.ent.etnc.dtos.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.ent.etnc.models.enums.ERole;

/**
 * Données d'entrée pour la création d'un utilisateur.
 */
public record UserCreateRequest(
        @NotBlank(message = "Le nom d'utilisateur est obligatoire")
        @Size(max = 100, message = "Le nom d'utilisateur ne doit pas dépasser 100 caractères")
        String username,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 6, max = 255, message = "Le mot de passe doit comporter au moins 6 caractères")
        String password,

        @NotNull(message = "Le rôle doit être renseigné")
        ERole role
) {}
