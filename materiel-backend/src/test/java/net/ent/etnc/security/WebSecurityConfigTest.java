package net.ent.etnc.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("WebSecurityConfig Unit Tests")
class WebSecurityConfigTest {

    private final RestAuthenticationEntryPoint authenticationEntryPoint = mock(RestAuthenticationEntryPoint.class);
    private final RestAccessDeniedHandler accessDeniedHandler = mock(RestAccessDeniedHandler.class);
    private final KeycloakJwtConverter keycloakJwtConverter = mock(KeycloakJwtConverter.class);

    private final WebSecurityConfig config = new WebSecurityConfig(
            authenticationEntryPoint, accessDeniedHandler, keycloakJwtConverter
    );

    @Test
    @DisplayName("passwordEncoder doit fournir un BCryptPasswordEncoder fonctionnel")
    void passwordEncoder_ProvidesBCryptEncoder() {
        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(encoder).isNotNull();
        String hash = encoder.encode("password123");
        assertThat(encoder.matches("password123", hash)).isTrue();
    }
}
