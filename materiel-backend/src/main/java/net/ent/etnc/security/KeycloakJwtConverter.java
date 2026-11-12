package net.ent.etnc.security;

import net.ent.etnc.models.User;
import net.ent.etnc.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Turns a Keycloak access token (MinDef Connect) into a Spring authentication.
 * Keycloak only proves the identity: the account is loaded from the database by its username
 * (claim preferred_username) and its roles come from the database, not from Keycloak.
 * The principal stays the persisted {@link User}, so UserService.currentUser() and the
 * {@code @PreAuthorize} rules work unchanged.
 * RG : RG-14 (roles bound to accounts), RG-19 (a disabled account is refused)
 */
@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    /**
     * Claim holding the Keycloak username, matched against users.username.
     */
    static final String USERNAME_CLAIM = "preferred_username";

    /**
     * Account repository.
     */
    private final UserRepository userRepository;

    /**
     * Creates the converter.
     */
    @Autowired
    public KeycloakJwtConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads the account named in the token.
     *
     * @throws DisabledException (401) if the account is unknown to the application or disabled
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String username = jwt.getClaimAsString(USERNAME_CLAIM);
        User user = userRepository.findByUsername(username == null ? "" : username)
                .filter(User::isEnabled)
                .orElseThrow(() -> new DisabledException("Compte inconnu de l'application ou désactivé : " + username));
        return new UsernamePasswordAuthenticationToken(user, jwt, user.getAuthorities());
    }
}
