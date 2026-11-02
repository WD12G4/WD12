package net.ent.etnc.controllers;

import jakarta.servlet.http.HttpServletRequest;
import net.ent.etnc.dtos.assemblers.UserDtoAssembler;
import net.ent.etnc.dtos.auth.LoginRequest;
import net.ent.etnc.dtos.user.UserResponse;
import net.ent.etnc.handlers.GlobalExceptionHandler;
import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.ERole;
import net.ent.etnc.security.RefreshTokenProperties;
import net.ent.etnc.security.exceptions.InvalidRefreshTokenException;
import net.ent.etnc.security.jwt.JwtUtils;
import net.ent.etnc.services.RefreshTokenService;
import net.ent.etnc.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Web Tests")
class AuthControllerTest {

    private static final String COOKIE_NAME = "materiel_refresh_token";

    private MockMvc mockMvc;
    private AuthController authController;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserService userService;

    @Mock
    private UserDtoAssembler userDtoAssembler;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        RefreshTokenProperties refreshTokenProperties = new RefreshTokenProperties();
        refreshTokenProperties.getCookie().setName(COOKIE_NAME);

        authController = new AuthController(
                authenticationManager,
                jwtUtils,
                refreshTokenService,
                refreshTokenProperties,
                userService,
                userDtoAssembler
        );

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("admin");
        sampleUser.setRole(ERole.ADMIN);
    }

    @Nested
    @DisplayName("Tests du endpoint /api/v1/auth/login/")
    class LoginTests {

        @Test
        @DisplayName("Connexion réussie : renvoie 200, Header Authorization Bearer et Cookie HttpOnly")
        void login_ValidCredentials_Returns200WithBearerAndCookie() throws Exception {
            String requestJson = """
                    {
                        "username": "admin",
                        "password": "admin123"
                    }
                    """;
            Authentication auth = new UsernamePasswordAuthenticationToken(sampleUser, null);

            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtUtils.generateJwtToken(auth)).thenReturn("mocked.jwt.token");
            when(refreshTokenService.createToken(eq(sampleUser), any(), any())).thenReturn("mocked-refresh-token");

            mockMvc.perform(post("/api/v1/auth/login/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.AUTHORIZATION, "Bearer mocked.jwt.token"))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(COOKIE_NAME + "=mocked-refresh-token")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
        }

        @Test
        @DisplayName("Identifiants invalides : renvoie 401 et nettoie le cookie")
        void login_InvalidCredentials_Returns401() throws Exception {
            String requestJson = """
                    {
                        "username": "admin",
                        "password": "wrongpassword"
                    }
                    """;

            when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/v1/auth/login/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
        }
    }

    @Nested
    @DisplayName("Tests de rotation du refresh token (refresh)")
    class RefreshTests {

        @Test
        @DisplayName("Rotation réussie : renvoie 200, Authorization Bearer et nouveau cookie")
        void refresh_ValidCookie_Returns200AndNewToken() {
            RefreshTokenService.RotateResult rotateResult =
                    new RefreshTokenService.RotateResult(sampleUser, "new-refresh-token");

            when(refreshTokenService.rotate(eq("valid-refresh-token"), any(), any())).thenReturn(rotateResult);
            when(jwtUtils.generateJwtTokenForUser(sampleUser)).thenReturn("renewed.jwt.token");

            HttpServletRequest request = new MockHttpServletRequest();
            ResponseEntity<Void> response = authController.refresh("valid-refresh-token", request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer renewed.jwt.token");
            assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains(COOKIE_NAME + "=new-refresh-token");
        }

        @Test
        @DisplayName("Token manquant ou vide : renvoie 401 et nettoie le cookie")
        void refresh_MissingCookie_Returns401() {
            HttpServletRequest request = new MockHttpServletRequest();

            ResponseEntity<Void> response = authController.refresh(null, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
        }

        @Test
        @DisplayName("Token invalide : renvoie 401 et nettoie le cookie")
        void refresh_InvalidToken_Returns401() {
            when(refreshTokenService.rotate(eq("bad-token"), any(), any()))
                    .thenThrow(new InvalidRefreshTokenException());

            HttpServletRequest request = new MockHttpServletRequest();
            ResponseEntity<Void> response = authController.refresh("bad-token", request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
        }
    }

    @Nested
    @DisplayName("Tests de déconnexion (logout)")
    class LogoutTests {

        @Test
        @DisplayName("Déconnexion : révoque le token et supprime le cookie")
        void logout_RevokesTokenAndClearsCookie() {
            ResponseEntity<Void> response = authController.logout("active-token");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
            verify(refreshTokenService).revokeToken("active-token");
        }
    }

    @Nested
    @DisplayName("Tests du endpoint /api/v1/auth/me/")
    class MeTests {

        @Test
        @DisplayName("Renvoie le profil de l'utilisateur connecté")
        void me_ReturnsUserProfile() throws Exception {
            UserResponse response = new UserResponse(1L, 0L, "admin", ERole.ADMIN, true);

            when(userService.currentUser()).thenReturn(sampleUser);
            when(userDtoAssembler.toDto(sampleUser)).thenReturn(response);

            mockMvc.perform(get("/api/v1/auth/me/"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("admin"))
                    .andExpect(jsonPath("$.role").value("ADMIN"));
        }
    }
}
