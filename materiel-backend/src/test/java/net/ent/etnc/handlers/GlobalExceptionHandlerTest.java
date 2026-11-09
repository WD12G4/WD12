package net.ent.etnc.handlers;

import jakarta.validation.ConstraintViolationException;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import net.ent.etnc.services.commons.ServiceException;
import net.ent.etnc.services.commons.VersionConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("handleAccessDenied doit renvoyer 401 Unauthorized pour un utilisateur anonyme ou non authentifié")
    void handleAccessDenied_WhenAnonymous_Returns401() {
        // Sans authentification
        ResponseEntity<Map<String, String>> response1 =
                exceptionHandler.handleAccessDenied(new AccessDeniedException("Denied"));
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response1.getBody()).containsEntry("error", "Unauthorized");

        // Avec jeton anonyme
        AnonymousAuthenticationToken anonToken = new AnonymousAuthenticationToken(
                "key", "anon", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(anonToken);

        ResponseEntity<Map<String, String>> response2 =
                exceptionHandler.handleAccessDenied(new AccessDeniedException("Denied"));
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response2.getBody()).containsEntry("error", "Unauthorized");
    }

    @Test
    @DisplayName("handleAccessDenied doit renvoyer 403 Forbidden pour un utilisateur authentifié")
    void handleAccessDenied_WhenAuthenticated_Returns403() {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                "user", "password", List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authToken);

        ResponseEntity<Map<String, String>> response =
                exceptionHandler.handleAccessDenied(new AccessDeniedException("Denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("error", "Forbidden");
    }

    @Test
    @DisplayName("handleBadRequest doit renvoyer 400 Bad Request pour ServiceException et IllegalArgumentException")
    void handleBadRequest_Returns400() {
        ResponseEntity<Map<String, String>> responseService =
                exceptionHandler.handleBadRequest(new ServiceException("Règle métier violée"));
        assertThat(responseService.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseService.getBody()).containsEntry("error", "Règle métier violée");

        ResponseEntity<Map<String, String>> responseIllegal =
                exceptionHandler.handleBadRequest(new IllegalArgumentException("Argument invalide"));
        assertThat(responseIllegal.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseIllegal.getBody()).containsEntry("error", "Argument invalide");

        ResponseEntity<Map<String, String>> responseConstraint =
                exceptionHandler.handleBadRequest(new ConstraintViolationException("Contrainte non respectée", Collections.emptySet()));
        assertThat(responseConstraint.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseConstraint.getBody()).containsEntry("error", "Contrainte non respectée");
    }

    @Test
    @DisplayName("handleNotFound doit renvoyer 404 Not Found pour ResourceNotFoundException")
    void handleNotFound_Returns404() {
        ResponseEntity<Map<String, String>> response =
                exceptionHandler.handleNotFound(new ResourceNotFoundException("Ressource absente"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Ressource absente");
    }

    @Test
    @DisplayName("handleConflict doit renvoyer 409 Conflict pour VersionConflictException et OptimisticLockingFailureException")
    void handleConflict_Returns409() {
        ResponseEntity<Map<String, String>> responseConflict =
                exceptionHandler.handleConflict(new VersionConflictException("Conflit de version"));
        assertThat(responseConflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(responseConflict.getBody()).containsEntry("error", "Conflit de version");

        ResponseEntity<Map<String, String>> responseOptimistic =
                exceptionHandler.handleConflict(new OptimisticLockingFailureException("Verrouillage optimiste"));
        assertThat(responseOptimistic.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(responseOptimistic.getBody()).containsEntry("error", "Verrouillage optimiste");
    }
}
