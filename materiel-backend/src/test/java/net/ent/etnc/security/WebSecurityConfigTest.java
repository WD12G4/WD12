package net.ent.etnc.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("WebSecurityConfig Unit Tests")
class WebSecurityConfigTest {

    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final RestAuthenticationEntryPoint authenticationEntryPoint = mock(RestAuthenticationEntryPoint.class);
    private final RestAccessDeniedHandler accessDeniedHandler = mock(RestAccessDeniedHandler.class);

    private final WebSecurityConfig config = new WebSecurityConfig(
            userDetailsService, authenticationEntryPoint, accessDeniedHandler
    );

    @Test
    @DisplayName("passwordEncoder doit fournir un BCryptPasswordEncoder fonctionnel")
    void passwordEncoder_ProvidesBCryptEncoder() {
        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(encoder).isNotNull();
        String hash = encoder.encode("password123");
        assertThat(encoder.matches("password123", hash)).isTrue();
    }

    @Test
    @DisplayName("secureRandom doit fournir une instance de SecureRandom")
    void secureRandom_ProvidesInstance() {
        SecureRandom random = config.secureRandom();

        assertThat(random).isNotNull();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        assertThat(bytes).hasSize(16);
    }

    @Test
    @DisplayName("clock doit fournir une horloge UTC")
    void clock_ProvidesUtcClock() {
        Clock clock = config.clock();

        assertThat(clock).isNotNull();
        assertThat(clock.getZone()).isEqualTo(java.time.ZoneOffset.UTC);
    }
}
