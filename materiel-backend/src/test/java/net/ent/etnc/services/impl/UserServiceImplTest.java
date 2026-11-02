package net.ent.etnc.services.impl;

import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.ERole;
import net.ent.etnc.repositories.UserRepository;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import net.ent.etnc.services.commons.ServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("testuser");
        sampleUser.setPassword("plainPassword123");
        sampleUser.setRole(ERole.USER);
        sampleUser.setEnabled(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Tests de création (create)")
    class CreateTests {

        @Test
        @DisplayName("Doit encoder le mot de passe et créer l'utilisateur")
        void create_Success() {
            when(repository.existsByUsername("testuser")).thenReturn(false);
            when(passwordEncoder.encode("plainPassword123")).thenReturn("encodedPasswordHash");
            when(repository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            User created = userService.create(sampleUser);

            assertThat(created).isNotNull();
            assertThat(created.getPassword()).isEqualTo("encodedPasswordHash");
            verify(passwordEncoder).encode("plainPassword123");
            verify(repository).saveAndFlush(sampleUser);
        }

        @Test
        @DisplayName("Doit lever ServiceException si le nom d'utilisateur est vide")
        void create_ThrowsException_WhenUsernameBlank() {
            sampleUser.setUsername("");

            assertThatThrownBy(() -> userService.create(sampleUser))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("Le nom d'utilisateur est obligatoire");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit lever ServiceException si le nom d'utilisateur existe déjà")
        void create_ThrowsException_WhenUsernameAlreadyExists() {
            when(repository.existsByUsername("testuser")).thenReturn(true);

            assertThatThrownBy(() -> userService.create(sampleUser))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("Le nom d'utilisateur est déjà utilisé");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit lever ServiceException si le mot de passe est vide")
        void create_ThrowsException_WhenPasswordBlank() {
            when(repository.existsByUsername("testuser")).thenReturn(false);
            sampleUser.setPassword("   ");

            assertThatThrownBy(() -> userService.create(sampleUser))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("Le mot de passe est obligatoire");

            verify(repository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("Tests de mise à jour (update)")
    class UpdateTests {

        @Test
        @DisplayName("Doit encoder le mot de passe s'il est en clair et sauvegarder l'utilisateur")
        void update_WhenPlainPassword_EncodesPasswordAndSaves() {
            when(repository.existsById(1L)).thenReturn(true);
            when(passwordEncoder.encode("plainPassword123")).thenReturn("encodedPasswordHash");
            when(repository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            User updated = userService.update(sampleUser);

            assertThat(updated).isNotNull();
            assertThat(updated.getPassword()).isEqualTo("encodedPasswordHash");
            verify(passwordEncoder).encode("plainPassword123");
            verify(repository).saveAndFlush(sampleUser);
        }

        @Test
        @DisplayName("Ne doit pas réencoder un mot de passe déjà sous forme de hash BCrypt")
        void update_WhenAlreadyHashed_DoesNotReEncode() {
            String bcryptHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
            sampleUser.setPassword(bcryptHash);
            when(repository.existsById(1L)).thenReturn(true);
            when(repository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            User updated = userService.update(sampleUser);

            assertThat(updated).isNotNull();
            assertThat(updated.getPassword()).isEqualTo(bcryptHash);
            verify(passwordEncoder, never()).encode(any());
            verify(repository).saveAndFlush(sampleUser);
        }

        @Test
        @DisplayName("Doit lever ResourceNotFoundException si l'utilisateur n'existe pas")
        void update_WhenNotFound_ThrowsException() {
            when(repository.existsById(1L)).thenReturn(false);

            assertThatThrownBy(() -> userService.update(sampleUser))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Utilisateur introuvable");

            verify(repository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("Tests de recherche par username (findByUsername)")
    class FindByUsernameTests {

        @Test
        @DisplayName("Doit retourner l'utilisateur lorsqu'il existe")
        void findByUsername_ReturnsUser() {
            when(repository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));

            Optional<User> found = userService.findByUsername("testuser");

            assertThat(found).isPresent().contains(sampleUser);
        }

        @Test
        @DisplayName("Doit retourner Optional.empty() lorsque l'utilisateur n'existe pas")
        void findByUsername_NotFound_ReturnsEmpty() {
            when(repository.findByUsername("unknown")).thenReturn(Optional.empty());

            Optional<User> found = userService.findByUsername("unknown");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tests de l'utilisateur courant (currentUser)")
    class CurrentUserTests {

        @Test
        @DisplayName("Doit renvoyer l'utilisateur connecté depuis le SecurityContext")
        void currentUser_Authenticated_ReturnsUser() {
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    sampleUser, null, sampleUser.getAuthorities());
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            when(repository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));

            User result = userService.currentUser();

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Doit lever AccessDeniedException si aucun utilisateur n'est authentifié")
        void currentUser_Unauthenticated_ThrowsAccessDenied() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> userService.currentUser())
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Utilisateur non authentifié");
        }

        @Test
        @DisplayName("Doit lever AccessDeniedException pour un token anonyme")
        void currentUser_Anonymous_ThrowsAccessDenied() {
            Authentication anon = new AnonymousAuthenticationToken(
                    "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(anon);
            SecurityContextHolder.setContext(context);

            assertThatThrownBy(() -> userService.currentUser())
                    .isInstanceOf(AccessDeniedException.class);
        }
    }
}
