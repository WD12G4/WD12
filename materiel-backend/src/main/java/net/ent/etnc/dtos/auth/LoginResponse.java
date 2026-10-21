package net.ent.etnc.dtos.auth;

/**
 * Réponse renvoyée après authentification réussie.
 */
public record LoginResponse(String accessToken, String tokenType, UserSummaryResponse user) {
    public LoginResponse(String accessToken, UserSummaryResponse user) {
        this(accessToken, "Bearer", user);
    }
}
