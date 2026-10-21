package net.ent.etnc.dtos.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Données requises pour l'authentification login.
 */
public record LoginRequest(
        @NotBlank(message = "Le nom d'utilisateur est obligatoire")
        String username,
        @NotBlank(message = "Le mot de passe est obligatoire")
        String password
) {
}
