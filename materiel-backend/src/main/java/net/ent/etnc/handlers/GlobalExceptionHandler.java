package net.ent.etnc.handlers;

import jakarta.validation.ConstraintViolationException;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import net.ent.etnc.services.commons.ServiceException;
import net.ent.etnc.services.commons.VersionConflictException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Maps application failures to stable HTTP error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * Maps method-security access denials to 401 for anonymous users and 403 for authenticated users.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException exception) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean anonymous = authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken;
        HttpStatus status = anonymous ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status).body(Map.of("error", status == HttpStatus.UNAUTHORIZED ? "Unauthorized" : "Forbidden"));
    }

    /**
     * Maps business-rule failures to HTTP 400.
     */
    @ExceptionHandler({ServiceException.class, ConstraintViolationException.class,
            MethodArgumentNotValidException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(Exception exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    /**
     * Maps missing resources to HTTP 404.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }

    /**
     * Maps stale versions to HTTP 409.
     */
    @ExceptionHandler({VersionConflictException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<Map<String, String>> handleConflict(Exception exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }
}


