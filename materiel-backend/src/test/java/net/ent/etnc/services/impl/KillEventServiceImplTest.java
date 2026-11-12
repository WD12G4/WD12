package net.ent.etnc.services.impl;

import net.ent.etnc.models.KillEvent;
import net.ent.etnc.models.Material;
import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.EMaterialType;
import net.ent.etnc.models.enums.ERole;
import net.ent.etnc.repositories.KillEventRepository;
import net.ent.etnc.repositories.MaterialRepository;
import net.ent.etnc.services.UserService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import net.ent.etnc.services.commons.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KillEventServiceImpl Unit Tests")
class KillEventServiceImplTest {

    @Mock
    private KillEventRepository repository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private KillEventServiceImpl killEventService;

    private Material sampleUc;
    private User sampleUser;
    private User sampleAdmin;
    private KillEvent sampleKillEvent;

    @BeforeEach
    void setUp() {
        sampleUc = new Material();
        sampleUc.setId(1L);
        sampleUc.setSerialNumber("UC-12345");
        sampleUc.setMaterialType(EMaterialType.UC);

        sampleUser = new User();
        sampleUser.setId(100L);
        sampleUser.setUsername("user1");
        sampleUser.setRole(ERole.USER);

        sampleAdmin = new User();
        sampleAdmin.setId(1L);
        sampleAdmin.setUsername("admin");
        sampleAdmin.setRole(ERole.ADMIN);

        sampleKillEvent = new KillEvent();
        sampleKillEvent.setId(10L);
        sampleKillEvent.setProcessName("notepad.exe");
        sampleKillEvent.setUc(sampleUc);
        sampleKillEvent.setUser(sampleUser);
        sampleKillEvent.setKillDateTime(LocalDateTime.now().minusMinutes(5));
        sampleKillEvent.setResult(true);
    }

    @Nested
    @DisplayName("Tests de création (create)")
    class CreateTests {

        @Test
        @DisplayName("Doit créer et retourner le KillEvent avec succès")
        void create_Success() {
            when(userService.findById(100L)).thenReturn(Optional.of(sampleUser));
            when(materialRepository.findById(1L)).thenReturn(Optional.of(sampleUc));
            when(repository.saveAndFlush(any(KillEvent.class))).thenReturn(sampleKillEvent);

            KillEvent created = killEventService.create(sampleKillEvent);

            assertThat(created).isNotNull();
            assertThat(created.getProcessName()).isEqualTo("notepad.exe");
            assertThat(created.getUc()).isEqualTo(sampleUc);
            assertThat(created.getUser()).isEqualTo(sampleUser);
            assertThat(created.getResult()).isTrue();
            verify(repository).saveAndFlush(sampleKillEvent);
        }

        @Test
        @DisplayName("Doit assigner currentUser si aucun utilisateur n'est renseigné")
        void create_AssignsCurrentUser_WhenUserNull() {
            sampleKillEvent.setUser(null);
            when(userService.currentUser()).thenReturn(sampleUser);
            when(materialRepository.findById(1L)).thenReturn(Optional.of(sampleUc));
            when(repository.saveAndFlush(any(KillEvent.class))).thenReturn(sampleKillEvent);

            KillEvent created = killEventService.create(sampleKillEvent);

            assertThat(created.getUser()).isEqualTo(sampleUser);
            verify(repository).saveAndFlush(sampleKillEvent);
        }

        @Test
        @DisplayName("Doit échouer si le nom du processus est null")
        void create_ThrowsException_WhenProcessNameNull() {
            sampleKillEvent.setProcessName(null);
            when(userService.findById(100L)).thenReturn(Optional.of(sampleUser));

            assertThatThrownBy(() -> killEventService.create(sampleKillEvent))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("nom du processus doit être renseigné");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit échouer si le nom du processus est vide")
        void create_ThrowsException_WhenProcessNameBlank() {
            sampleKillEvent.setProcessName("   ");
            when(userService.findById(100L)).thenReturn(Optional.of(sampleUser));

            assertThatThrownBy(() -> killEventService.create(sampleKillEvent))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("nom du processus doit être renseigné");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit échouer si le nom du processus dépasse 100 caractères")
        void create_ThrowsException_WhenProcessNameTooLong() {
            sampleKillEvent.setProcessName("a".repeat(101));
            when(userService.findById(100L)).thenReturn(Optional.of(sampleUser));

            assertThatThrownBy(() -> killEventService.create(sampleKillEvent))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("inférieur à 100 caractères");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit échouer si l'UC est null")
        void create_ThrowsException_WhenUcNull() {
            sampleKillEvent.setUc(null);
            when(userService.findById(100L)).thenReturn(Optional.of(sampleUser));

            assertThatThrownBy(() -> killEventService.create(sampleKillEvent))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("L'UC doit être renseignée");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit échouer si l'ID de l'UC est null")
        void create_ThrowsException_WhenUcIdNull() {
            Material ucWithoutId = new Material();
            sampleKillEvent.setUc(ucWithoutId);
            when(userService.findById(100L)).thenReturn(Optional.of(sampleUser));

            assertThatThrownBy(() -> killEventService.create(sampleKillEvent))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("L'UC doit être renseignée");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit renvoyer ResourceNotFoundException si l'UC n'existe pas en BDD")
        void create_ThrowsException_WhenUcNotFound() {
            when(userService.findById(100L)).thenReturn(Optional.of(sampleUser));
            when(materialRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> killEventService.create(sampleKillEvent))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("UC non trouvée avec l'id : 1");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit échouer si le matériel n'est pas une UC")
        void create_ThrowsException_WhenMaterialIsNotUc() {
            when(userService.findById(100L)).thenReturn(Optional.of(sampleUser));
            Material screen = new Material();
            screen.setId(1L);
            screen.setMaterialType(EMaterialType.SCREEN);
            when(materialRepository.findById(1L)).thenReturn(Optional.of(screen));

            assertThatThrownBy(() -> killEventService.create(sampleKillEvent))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("doit être une UC");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit échouer si la date/heure d'arrêt est null")
        void create_ThrowsException_WhenKillDateTimeNull() {
            when(userService.findById(100L)).thenReturn(Optional.of(sampleUser));
            when(materialRepository.findById(1L)).thenReturn(Optional.of(sampleUc));
            sampleKillEvent.setKillDateTime(null);

            assertThatThrownBy(() -> killEventService.create(sampleKillEvent))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("L'heure et la date doivent être renseignées");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit échouer si la date/heure d'arrêt est dans le futur")
        void create_ThrowsException_WhenKillDateTimeInFuture() {
            when(userService.findById(100L)).thenReturn(Optional.of(sampleUser));
            when(materialRepository.findById(1L)).thenReturn(Optional.of(sampleUc));
            sampleKillEvent.setKillDateTime(LocalDateTime.now().plusDays(1));

            assertThatThrownBy(() -> killEventService.create(sampleKillEvent))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("ne peut pas être après le jour/l'heure actuelle");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit échouer si le résultat de l'arrêt est null")
        void create_ThrowsException_WhenResultNull() {
            when(userService.findById(100L)).thenReturn(Optional.of(sampleUser));
            when(materialRepository.findById(1L)).thenReturn(Optional.of(sampleUc));
            sampleKillEvent.setResult(null);

            assertThatThrownBy(() -> killEventService.create(sampleKillEvent))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("résultat de l'arrêt doit être renseigné");

            verify(repository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("Tests des droits d'accès et filtrages (findAll, findByUserId, findById)")
    class AccessControlTests {

        private final Pageable pageable = PageRequest.of(0, 20);

        @Test
        @DisplayName("ADMIN voit tous les KillEvents dans findAll")
        void findAll_Admin_ReturnsAllEvents() {
            when(userService.currentUser()).thenReturn(sampleAdmin);
            when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(sampleKillEvent)));

            Page<KillEvent> result = killEventService.findAll(pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(repository).findAll(pageable);
            verify(repository, never()).findByUser_Id(anyLong(), any());
        }

        @Test
        @DisplayName("USER ne voit que ses propres KillEvents dans findAll")
        void findAll_User_ReturnsOnlyUserEvents() {
            when(userService.currentUser()).thenReturn(sampleUser);
            when(repository.findByUser_Id(100L, pageable)).thenReturn(new PageImpl<>(List.of(sampleKillEvent)));

            Page<KillEvent> result = killEventService.findAll(pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(repository).findByUser_Id(100L, pageable);
            verify(repository, never()).findAll(pageable);
        }

        @Test
        @DisplayName("MANAGER ne voit que ses propres KillEvents dans findAll")
        void findAll_Manager_ReturnsOnlyManagerEvents() {
            User manager = new User();
            manager.setId(200L);
            manager.setUsername("manager1");
            manager.setRole(ERole.MANAGER);

            when(userService.currentUser()).thenReturn(manager);
            when(repository.findByUser_Id(200L, pageable)).thenReturn(new PageImpl<>(List.of(sampleKillEvent)));

            Page<KillEvent> result = killEventService.findAll(pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(repository).findByUser_Id(200L, pageable);
            verify(repository, never()).findAll(pageable);
        }

        @Test
        @DisplayName("ADMIN peut interroger findByUserId pour n'importe quel utilisateur")
        void findByUserId_Admin_CanViewAnyUser() {
            when(userService.currentUser()).thenReturn(sampleAdmin);
            when(repository.findByUser_Id(100L, pageable)).thenReturn(new PageImpl<>(List.of(sampleKillEvent)));

            Page<KillEvent> result = killEventService.findByUserId(100L, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(repository).findByUser_Id(100L, pageable);
        }

        @Test
        @DisplayName("USER peut interroger findByUserId pour son propre compte")
        void findByUserId_User_CanViewOwnEvents() {
            when(userService.currentUser()).thenReturn(sampleUser);
            when(repository.findByUser_Id(100L, pageable)).thenReturn(new PageImpl<>(List.of(sampleKillEvent)));

            Page<KillEvent> result = killEventService.findByUserId(100L, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(repository).findByUser_Id(100L, pageable);
        }

        @Test
        @DisplayName("USER se voit refuser l'accès s'il demande les KillEvents d'un autre utilisateur")
        void findByUserId_User_AccessDeniedForOtherUser() {
            when(userService.currentUser()).thenReturn(sampleUser);

            assertThatThrownBy(() -> killEventService.findByUserId(999L, pageable))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("pas autorisé à consulter les KillEvents d'un autre utilisateur");

            verify(repository, never()).findByUser_Id(anyLong(), any());
        }

        @Test
        @DisplayName("ADMIN peut accéder à n'importe quel KillEvent par son ID")
        void findById_Admin_CanAccessAny() {
            when(repository.findById(10L)).thenReturn(Optional.of(sampleKillEvent));
            when(userService.currentUser()).thenReturn(sampleAdmin);

            Optional<KillEvent> result = killEventService.findById(10L);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(sampleKillEvent);
        }

        @Test
        @DisplayName("USER peut accéder à son propre KillEvent par son ID")
        void findById_User_CanAccessOwnEvent() {
            when(repository.findById(10L)).thenReturn(Optional.of(sampleKillEvent));
            when(userService.currentUser()).thenReturn(sampleUser);

            Optional<KillEvent> result = killEventService.findById(10L);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(sampleKillEvent);
        }

        @Test
        @DisplayName("USER se voit refuser l'accès au KillEvent d'un autre utilisateur")
        void findById_User_AccessDeniedForOtherUserEvent() {
            User anotherUser = new User();
            anotherUser.setId(999L);
            anotherUser.setUsername("hacker");
            anotherUser.setRole(ERole.USER);

            when(repository.findById(10L)).thenReturn(Optional.of(sampleKillEvent));
            when(userService.currentUser()).thenReturn(anotherUser);

            assertThatThrownBy(() -> killEventService.findById(10L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("pas accès à ce KillEvent");
        }
    }

    @Nested
    @DisplayName("Tests des opérations non supportées")
    class UnsupportedOperationsTests {

        @Test
        @DisplayName("update doit lever UnsupportedOperationException")
        void update_ThrowsUnsupportedOperationException() {
            assertThatThrownBy(() -> killEventService.update(sampleKillEvent))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("mise à jour d'un Kill Event n'est pas autorisée");
        }

        @Test
        @DisplayName("deleteById doit lever UnsupportedOperationException")
        void deleteById_ThrowsUnsupportedOperationException() {
            assertThatThrownBy(() -> killEventService.deleteById(10L))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("suppression d'un Kill Event n'est pas autorisée");
        }
    }
}
