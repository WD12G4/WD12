package net.ent.etnc.security;

import jakarta.servlet.http.HttpServletRequest;
import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.ERole;
import net.ent.etnc.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Turns a Keycloak access token (MinDef Connect) into a Spring authentication.
 * Keycloak only proves the identity: the account is loaded from the database by its username
 * (claim preferred_username) and its roles come from the database, not from Keycloak.
 * The principal stays the persisted {@link User}, so UserService.currentUser() and the
 * {@code @PreAuthorize} rules work unchanged.
 * DEMONSTRATION TOOL: an administrator may act as another demonstration account (header
 * {@value #DEMO_ACCOUNT_HEADER}) to test its role without logging out of Keycloak. Accepted only in
 * demonstration mode (app.demo.enabled) and only when the token belongs to an ADMIN account.
 * RG : RG-14 (roles bound to accounts), RG-19 (a disabled account is refused)
 */
@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    /**
     * Claim holding the Keycloak username, matched against users.username.
     */
    static final String USERNAME_CLAIM = "preferred_username";
    /**
     * Header naming the demonstration account to act as (sent by the frontend's account switch).
     */
    static final String DEMO_ACCOUNT_HEADER = "X-Demo-Account";

    /**
     * Account repository.
     */
    private final UserRepository userRepository;
    /**
     * True if an administrator may act as a demonstration account.
     */
    private final boolean demoEnabled;
    /**
     * Usernames an administrator may act as.
     */
    private final List<String> demoAccounts;

    /**
     * Creates the converter.
     */
    @Autowired
    public KeycloakJwtConverter(UserRepository userRepository,
                                @Value("${app.demo.enabled:true}") boolean demoEnabled,
                                @Value("${app.demo.accounts:admin,manager,user}") List<String> demoAccounts) {
        this.userRepository = userRepository;
        this.demoEnabled = demoEnabled;
        this.demoAccounts = List.copyOf(demoAccounts);
    }

    /**
     * Loads the account named in the token, or the demonstration account an administrator acts as.
     *
     * @throws DisabledException (401) if the account is unknown to the application or disabled
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String username = jwt.getClaimAsString(USERNAME_CLAIM);
        User user = findEnabled(username)
                .orElseThrow(() -> new DisabledException("Compte inconnu de l'application ou désactivé : " + username));
        User effective = actingAs(user).orElse(user);
        return new UsernamePasswordAuthenticationToken(effective, jwt, effective.getAuthorities());
    }

    /**
     * Demonstration account requested by an administrator, if allowed.
     * Any other case (mode off, not an administrator, unknown or disabled account) keeps the real account.
     */
    private Optional<User> actingAs(User user) {
        String requested = demoAccountHeader();
        if (!demoEnabled || requested == null || requested.isBlank()
                || user.getRole() != ERole.ADMIN || !demoAccounts.contains(requested)) {
            return Optional.empty();
        }
        return findEnabled(requested);
    }

    /**
     * Enabled account with this username.
     */
    private Optional<User> findEnabled(String username) {
        return userRepository.findByUsername(username == null ? "" : username).filter(User::isEnabled);
    }

    /**
     * Value of the demonstration header for the current request, or null outside a request.
     */
    private static String demoAccountHeader() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return request.getHeader(DEMO_ACCOUNT_HEADER);
        }
        return null;
    }
}
