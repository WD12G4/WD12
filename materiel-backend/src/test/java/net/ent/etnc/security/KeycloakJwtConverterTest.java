package net.ent.etnc.security;

import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.ERole;
import net.ent.etnc.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeycloakJwtConverter Unit Tests")
class KeycloakJwtConverterTest {

    private static final List<String> DEMO_ACCOUNTS = List.of("admin", "manager", "user");

    @Mock
    private UserRepository userRepository;

    private KeycloakJwtConverter converter;

    private User admin;
    private User manager;

    @BeforeEach
    void setUp() {
        converter = new KeycloakJwtConverter(userRepository, true, DEMO_ACCOUNTS);
        admin = account(1L, "admin", ERole.ADMIN);
        manager = account(2L, "manager", ERole.MANAGER);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    /** Compte actif de la base. */
    private static User account(Long id, String username, ERole role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        user.setEnabled(true);
        return user;
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

    /** Requête en cours portant l'en-tête de démonstration. */
    private static void requestActingAs(String username) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(KeycloakJwtConverter.DEMO_ACCOUNT_HEADER, username);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    @DisplayName("Doit authentifier le compte de la base avec ses rôles")
    void convert_KnownEnabledAccount_ReturnsAuthenticationWithDatabaseRoles() {
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));

        AbstractAuthenticationToken authentication = converter.convert(tokenFor("manager"));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isSameAs(manager);
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
        manager.setEnabled(false);
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> converter.convert(tokenFor("manager")))
                .isInstanceOf(DisabledException.class);
    }

    @Nested
    @DisplayName("Outil de démonstration : en-tête X-Demo-Account")
    class DemoAccountTests {

        @Test
        @DisplayName("Un administrateur agit avec le compte de démonstration demandé")
        void convert_AdminWithDemoHeader_ActsAsRequestedAccount() {
            requestActingAs("manager");
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
            when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));

            AbstractAuthenticationToken authentication = converter.convert(tokenFor("admin"));

            assertThat(authentication.getPrincipal()).isSameAs(manager);
            assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                    .contains("ROLE_MANAGER")
                    .doesNotContain("ROLE_ADMIN");
        }

        @Test
        @DisplayName("Un compte qui n'est pas administrateur garde son propre compte")
        void convert_NonAdminWithDemoHeader_KeepsOwnAccount() {
            requestActingAs("admin");
            when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));

            AbstractAuthenticationToken authentication = converter.convert(tokenFor("manager"));

            assertThat(authentication.getPrincipal()).isSameAs(manager);
        }

        @Test
        @DisplayName("Un compte hors de la liste de démonstration est ignoré")
        void convert_HeaderNotADemoAccount_KeepsOwnAccount() {
            requestActingAs("intrus");
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

            AbstractAuthenticationToken authentication = converter.convert(tokenFor("admin"));

            assertThat(authentication.getPrincipal()).isSameAs(admin);
        }

        @Test
        @DisplayName("Un compte de démonstration désactivé est ignoré")
        void convert_DisabledDemoAccount_KeepsOwnAccount() {
            requestActingAs("manager");
            manager.setEnabled(false);
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
            when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));

            AbstractAuthenticationToken authentication = converter.convert(tokenFor("admin"));

            assertThat(authentication.getPrincipal()).isSameAs(admin);
        }

        @Test
        @DisplayName("Hors mode démonstration, l'en-tête est ignoré")
        void convert_DemoDisabled_IgnoresHeader() {
            converter = new KeycloakJwtConverter(userRepository, false, DEMO_ACCOUNTS);
            requestActingAs("manager");
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
            lenient().when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));

            AbstractAuthenticationToken authentication = converter.convert(tokenFor("admin"));

            assertThat(authentication.getPrincipal()).isSameAs(admin);
        }
    }
}
