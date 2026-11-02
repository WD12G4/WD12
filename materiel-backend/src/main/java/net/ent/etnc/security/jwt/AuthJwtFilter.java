package net.ent.etnc.security.jwt;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads Bearer tokens and installs a valid Spring Security authentication.
 */
@Component
public class AuthJwtFilter extends OncePerRequestFilter {
    /**
     * JWT helper.
     */
    private final JwtUtils jwtUtils;
    /**
     * User lookup service.
     */
    private final UserDetailsService userDetailsService;

    /**
     * Creates the filter.
     */
    @Autowired
    public AuthJwtFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Validates the bearer token without writing error responses directly.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = getJwtFromRequest(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                jwtUtils.getUsernameIfValid(token).ifPresent(username -> {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            } catch (AuthenticationException exception) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts a bearer token from the Authorization header.
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        return bearerToken != null && bearerToken.startsWith("Bearer ") ? bearerToken.substring(7).trim() : null;
    }
}
