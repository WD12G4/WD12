package net.ent.etnc.controllers;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import net.ent.etnc.dtos.assemblers.UserDtoAssembler;
import net.ent.etnc.dtos.auth.LoginRequest;
import net.ent.etnc.dtos.user.UserResponse;
import net.ent.etnc.models.User;
import net.ent.etnc.security.RefreshTokenProperties;
import net.ent.etnc.security.exceptions.ExpiredRefreshTokenException;
import net.ent.etnc.security.exceptions.InvalidRefreshTokenException;
import net.ent.etnc.security.exceptions.ReusedRefreshTokenException;
import net.ent.etnc.security.jwt.JwtUtils;
import net.ent.etnc.services.RefreshTokenService;
import net.ent.etnc.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints for access-token and refresh-cookie lifecycle.
 */
@RestController
@RequestMapping("/api/v1/auth/")
public class AuthController {
    /**
     * Spring authentication manager.
     */
    private final AuthenticationManager authenticationManager;
    /**
     * JWT helper.
     */
    private final JwtUtils jwtUtils;
    /**
     * Refresh token service.
     */
    private final RefreshTokenService refreshTokenService;
    /**
     * Refresh token settings.
     */
    private final RefreshTokenProperties refreshTokenProperties;
    /**
     * Current user lookup.
     */
    private final UserService userService;
    /**
     * User response assembler.
     */
    private final UserDtoAssembler userDtoAssembler;

    /**
     * Creates the auth controller.
     */
    @Autowired
    public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils,
                          RefreshTokenService refreshTokenService, RefreshTokenProperties refreshTokenProperties,
                          UserService userService, UserDtoAssembler userDtoAssembler) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenProperties = refreshTokenProperties;
        this.userService = userService;
        this.userDtoAssembler = userDtoAssembler;
    }

    /**
     * Authenticates credentials and returns an access token header plus refresh cookie.
     */
    @PostMapping("login/")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest payload, HttpServletRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(payload.username(), payload.password()));
            User user = (User) authentication.getPrincipal();
            String accessToken = jwtUtils.generateJwtToken(authentication);
            String rawRefreshToken = refreshTokenService.createToken(user, extractIp(request), request.getHeader(HttpHeaders.USER_AGENT));
            return ResponseEntity.ok()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(rawRefreshToken).toString())
                    .build();
        } catch (AuthenticationException exception) {
            return unauthorizedAndClearCookie();
        }
    }

    /**
     * Rotates the refresh cookie and returns a fresh access token header.
     */
    @PostMapping("refresh/")
    public ResponseEntity<Void> refresh(@CookieValue(name = "${app.security.refresh-token.cookie.name:object_rent_refresh}", required = false) String rawToken,
                                        HttpServletRequest request) {
        if (rawToken == null || rawToken.isBlank()) return unauthorizedAndClearCookie();
        try {
            RefreshTokenService.RotateResult result = refreshTokenService.rotate(rawToken, extractIp(request), request.getHeader(HttpHeaders.USER_AGENT));
            String accessToken = jwtUtils.generateJwtTokenForUser(result.user());
            return ResponseEntity.ok()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.newRawToken()).toString())
                    .build();
        } catch (InvalidRefreshTokenException | ExpiredRefreshTokenException | ReusedRefreshTokenException exception) {
            return unauthorizedAndClearCookie();
        }
    }

    /**
     * Revokes the current refresh cookie idempotently.
     */
    @PostMapping("logout/")
    public ResponseEntity<Void> logout(@CookieValue(name = "${app.security.refresh-token.cookie.name:object_rent_refresh}", required = false) String rawToken) {
        refreshTokenService.revokeToken(rawToken);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, buildClearCookie().toString()).build();
    }

    /**
     * Returns the authenticated user profile.
     */
    @GetMapping("me/")
    public UserResponse me() {
        return userDtoAssembler.toDto(userService.currentUser());
    }

    /**
     * Builds a refresh cookie with the raw token value.
     */
    private ResponseCookie buildRefreshCookie(String rawToken) {
        RefreshTokenProperties.Cookie cookie = refreshTokenProperties.getCookie();
        return ResponseCookie.from(cookie.getName(), rawToken)
                .httpOnly(true).secure(cookie.isSecure()).sameSite(cookie.getSameSite())
                .path(cookie.getPath()).maxAge(refreshTokenProperties.getExpiration()).build();
    }

    /**
     * Builds a cookie deletion response with identical attributes.
     */
    private ResponseCookie buildClearCookie() {
        RefreshTokenProperties.Cookie cookie = refreshTokenProperties.getCookie();
        return ResponseCookie.from(cookie.getName(), "")
                .httpOnly(true).secure(cookie.isSecure()).sameSite(cookie.getSameSite())
                .path(cookie.getPath()).maxAge(0).build();
    }

    /**
     * Returns 401 and clears the refresh cookie.
     */
    private ResponseEntity<Void> unauthorizedAndClearCookie() {
        return ResponseEntity.status(401).header(HttpHeaders.SET_COOKIE, buildClearCookie().toString()).build();
    }

    /**
     * Extracts audit IP, trusting forwarded headers only when configured.
     */
    private String extractIp(HttpServletRequest request) {
        if (refreshTokenProperties.getAudit().isTrustForwardedFor()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
