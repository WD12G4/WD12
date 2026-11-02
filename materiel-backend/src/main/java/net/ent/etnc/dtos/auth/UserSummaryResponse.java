package net.ent.etnc.dtos.auth;

import net.ent.etnc.models.enums.ERole;

/**
 * Données publiques résumées d'un utilisateur.
 */
public record UserSummaryResponse(Long id, String username, ERole role) {
}
