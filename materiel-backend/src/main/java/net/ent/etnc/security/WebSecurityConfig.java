package net.ent.etnc.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Central Spring Security configuration: stateless resource server.
 * Access tokens are issued by Keycloak (MinDef Connect) and checked against its public keys
 * (spring.security.oauth2.resourceserver.jwt in application.yml); the account and its roles are
 * loaded from the database by {@link KeycloakJwtConverter}.
 */
@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {
    /**
     * JSON 401 entry point.
     */
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    /**
     * JSON 403 handler.
     */
    private final RestAccessDeniedHandler accessDeniedHandler;
    /**
     * Keycloak token to application account.
     */
    private final KeycloakJwtConverter keycloakJwtConverter;

    /**
     * Creates the security configuration.
     */
    @Autowired
    public WebSecurityConfig(RestAuthenticationEntryPoint authenticationEntryPoint,
                             RestAccessDeniedHandler accessDeniedHandler,
                             KeycloakJwtConverter keycloakJwtConverter) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.keycloakJwtConverter = keycloakJwtConverter;
    }

    /**
     * Defines URL authorization and validates Keycloak bearer tokens.
     */
    @Bean
    @SuppressWarnings("java:S4502") // Désactivation CSRF intentionnelle : API REST Stateless basée sur des tokens JWT
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/ws/agent", "/ws/agent/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return httpSecurity.build();
    }
}
