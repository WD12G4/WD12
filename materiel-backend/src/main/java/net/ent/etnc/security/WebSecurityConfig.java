package net.ent.etnc.security;

import net.ent.etnc.security.jwt.AuthJwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.security.SecureRandom;
import java.time.Clock;

/**
 * Central Spring Security configuration for stateless JWT authentication.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(RefreshTokenProperties.class)
public class WebSecurityConfig {
    /**
     * User lookup service.
     */
    private final UserDetailsService userDetailsService;
    /**
     * JSON 401 entry point.
     */
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    /**
     * JSON 403 handler.
     */
    private final RestAccessDeniedHandler accessDeniedHandler;

    /**
     * Creates the security configuration.
     */
    @Autowired
    public WebSecurityConfig(UserDetailsService userDetailsService,
                             RestAuthenticationEntryPoint authenticationEntryPoint,
                             RestAccessDeniedHandler accessDeniedHandler) {
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    /**
     * Password encoder used for stored account passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication manager backed by the persisted UserDetailsService.
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity httpSecurity) throws Exception {
        AuthenticationManagerBuilder builder = httpSecurity.getSharedObject(AuthenticationManagerBuilder.class);
        builder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        return builder.build();
    }

    /**
     * Secure random source for refresh token generation.
     */
    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }

    /**
     * UTC clock used by token services.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Defines URL authorization and installs the JWT filter.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, AuthJwtFilter authJwtFilter) throws Exception {
        httpSecurity
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(authJwtFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }
}
