package net.ent.etnc.security;

import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.ERole;
import net.ent.etnc.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeycloakJwtConverter Unit Tests")
class KeycloakJwtConverterTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private KeycloakJwtConverter converter;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("manager");
        sampleUser.setPassword("hashedPassword");
        sampleUser.setRole(ERole.MANAGER);
        sampleUser.setEnabled(true);
    }

    /** Jeton Keycloak minimal portant l'identifiant donné. */
    private static Jwt tokenFor(String username) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim(KeycloakJwtConverter.USERNAME_CLAIM, username)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Test
    @DisplayName("Doit authentifier le compte de la base avec ses rôles")
    void convert_KnownEnabledAccount_ReturnsAuthenticationWithDatabaseRoles() {
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(sampleUser));

        AbstractAuthenticationToken authentication = converter.convert(tokenFor("manager"));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isSameAs(sampleUser);
        assertThat(authentication.getName()).isEqualTo("manager");
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_MANAGER");
    }

    @Test
    @DisplayName("Doit refuser un compte inconnu de l'application")
    void convert_UnknownAccount_ThrowsDisabledException() {
        when(userRepository.findByUsername("inconnu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> converter.convert(tokenFor("inconnu")))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("inconnu");
    }

    @Test
    @DisplayName("Doit refuser un compte désactivé (RG-19)")
    void convert_DisabledAccount_ThrowsDisabledException() {
        sampleUser.setEnabled(false);
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> converter.convert(tokenFor("manager")))
                .isInstanceOf(DisabledException.class);
    }
}
