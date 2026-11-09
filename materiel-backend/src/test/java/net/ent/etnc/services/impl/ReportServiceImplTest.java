package net.ent.etnc.services.impl;

import net.ent.etnc.models.Material;
import net.ent.etnc.models.Report;
import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.EMaterialType;
import net.ent.etnc.models.enums.ERole;
import net.ent.etnc.repositories.MaterialRepository;
import net.ent.etnc.repositories.ReportRepository;
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
@DisplayName("ReportServiceImpl Unit Tests")
class ReportServiceImplTest {

    @Mock
    private ReportRepository repository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReportServiceImpl reportService;

    private User creator;
    private User otherUser;
    private User managerUser;
    private Material sampleMaterial;
    private Report sampleReport;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(1L);
        creator.setUsername("john_doe");
        creator.setRole(ERole.USER);

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("jane_doe");
        otherUser.setRole(ERole.USER);

        managerUser = new User();
        managerUser.setId(3L);
        managerUser.setUsername("manager_bob");
        managerUser.setRole(ERole.MANAGER);

        sampleMaterial = new Material();
        sampleMaterial.setId(10L);
        sampleMaterial.setSerialNumber("UC-1000");
        sampleMaterial.setMaterialType(EMaterialType.UC);

        sampleReport = new Report();
        sampleReport.setId(100L);
        sampleReport.setDescription("Problème d'affichage");
        sampleReport.setUser(creator);
        sampleReport.setMaterial(sampleMaterial);
        sampleReport.setReportDateTime(LocalDateTime.now().minusHours(1));
        sampleReport.setHandled(false);
        sampleReport.setHandleDateTime(null);
    }

    @Nested
    @DisplayName("Tests de création (create)")
    class CreateTests {

        @Test
        @DisplayName("Doit créer et retourner le signalement avec succès")
        void create_Success() {
            when(userService.currentUser()).thenReturn(creator);
            when(materialRepository.findById(10L)).thenReturn(Optional.of(sampleMaterial));
            when(repository.saveAndFlush(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Report input = new Report();
            input.setDescription("Écran noir");
            Material matRef = new Material();
            matRef.setId(10L);
            input.setMaterial(matRef);

            Report created = reportService.create(input);

            assertThat(created).isNotNull();
            assertThat(created.getDescription()).isEqualTo("Écran noir");
            assertThat(created.getUser()).isEqualTo(creator);
            assertThat(created.getMaterial()).isEqualTo(sampleMaterial);
            assertThat(created.isHandled()).isFalse();
            assertThat(created.getHandleDateTime()).isNull();
            assertThat(created.getReportDateTime()).isNotNull();

            verify(repository).saveAndFlush(input);
        }

        @Test
        @DisplayName("Doit échouer si le matériel est null")
        void create_ThrowsException_WhenMaterialNull() {
            when(userService.currentUser()).thenReturn(creator);

            Report input = new Report();
            input.setDescription("Écran noir");
            input.setMaterial(null);

            assertThatThrownBy(() -> reportService.create(input))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("matériel doit être renseigné");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit échouer si l'ID du matériel est null")
        void create_ThrowsException_WhenMaterialIdNull() {
            when(userService.currentUser()).thenReturn(creator);

            Report input = new Report();
            input.setDescription("Écran noir");
            input.setMaterial(new Material());

            assertThatThrownBy(() -> reportService.create(input))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("matériel doit être renseigné");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit échouer si le matériel n'existe pas en base")
        void create_ThrowsException_WhenMaterialNotFound() {
            when(userService.currentUser()).thenReturn(creator);
            when(materialRepository.findById(10L)).thenReturn(Optional.empty());

            Report input = new Report();
            input.setDescription("Écran noir");
            Material matRef = new Material();
            matRef.setId(10L);
            input.setMaterial(matRef);

            assertThatThrownBy(() -> reportService.create(input))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Matériel introuvable avec l'id : 10");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit échouer si la description est vide")
        void create_ThrowsException_WhenDescriptionBlank() {
            when(userService.currentUser()).thenReturn(creator);
            when(materialRepository.findById(10L)).thenReturn(Optional.of(sampleMaterial));

            Report input = new Report();
            input.setDescription("   ");
            Material matRef = new Material();
            matRef.setId(10L);
            input.setMaterial(matRef);

            assertThatThrownBy(() -> reportService.create(input))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("description doit être renseignée");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit échouer si la description dépasse 500 caractères")
        void create_ThrowsException_WhenDescriptionTooLong() {
            when(userService.currentUser()).thenReturn(creator);
            when(materialRepository.findById(10L)).thenReturn(Optional.of(sampleMaterial));

            Report input = new Report();
            input.setDescription("x".repeat(501));
            Material matRef = new Material();
            matRef.setId(10L);
            input.setMaterial(matRef);

            assertThatThrownBy(() -> reportService.create(input))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("ne doit pas dépasser 500 caractères");

            verify(repository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("Tests de modification (update)")
    class UpdateTests {

        @Test
        @DisplayName("Le créateur doit pouvoir modifier la description avec succès")
        void update_Success_WhenCurrentUserIsCreator() {
            when(repository.findById(100L)).thenReturn(Optional.of(sampleReport));
            when(userService.currentUser()).thenReturn(creator);
            when(repository.existsById(100L)).thenReturn(true);
            when(repository.saveAndFlush(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Report updateInput = new Report();
            updateInput.setId(100L);
            updateInput.setDescription("Nouvelle description mise à jour");

            Report updated = reportService.update(updateInput);

            assertThat(updated).isNotNull();
            assertThat(updated.getDescription()).isEqualTo("Nouvelle description mise à jour");
            assertThat(updated.getUser()).isEqualTo(creator);
            assertThat(updated.getMaterial()).isEqualTo(sampleMaterial);
            verify(repository).saveAndFlush(any(Report.class));
        }

        @Test
        @DisplayName("Doit refuser la modification si l'utilisateur connecté n'est pas le créateur")
        void update_ThrowsAccessDenied_WhenUserNotCreator() {
            when(repository.findById(100L)).thenReturn(Optional.of(sampleReport));
            when(userService.currentUser()).thenReturn(otherUser);

            Report updateInput = new Report();
            updateInput.setId(100L);
            updateInput.setDescription("Tentative de hack");

            assertThatThrownBy(() -> reportService.update(updateInput))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Seul l'utilisateur ayant créé le signalement peut le modifier");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit refuser la modification si le signalement est déjà traité")
        void update_ThrowsServiceException_WhenAlreadyHandled() {
            sampleReport.setHandled(true);
            sampleReport.setHandleDateTime(LocalDateTime.now());
            when(repository.findById(100L)).thenReturn(Optional.of(sampleReport));
            when(userService.currentUser()).thenReturn(creator);

            Report updateInput = new Report();
            updateInput.setId(100L);
            updateInput.setDescription("Tentative modification après traitement");

            assertThatThrownBy(() -> reportService.update(updateInput))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("Impossible de modifier un signalement déjà traité");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit renvoyer 404 si le signalement à modifier n'existe pas")
        void update_ThrowsResourceNotFound_WhenReportNotFound() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            Report updateInput = new Report();
            updateInput.setId(999L);
            updateInput.setDescription("Description");

            assertThatThrownBy(() -> reportService.update(updateInput))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Signalement introuvable");

            verify(repository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("Tests de traitement (markAsHandled)")
    class MarkAsHandledTests {

        @Test
        @DisplayName("Doit marquer le signalement comme traité avec la date actuelle")
        void markAsHandled_True_Success() {
            when(repository.findById(100L)).thenReturn(Optional.of(sampleReport));
            when(repository.saveAndFlush(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Report handledReport = reportService.markAsHandled(100L, true);

            assertThat(handledReport.isHandled()).isTrue();
            assertThat(handledReport.getHandleDateTime()).isNotNull();
            verify(repository).saveAndFlush(sampleReport);
        }

        @Test
        @DisplayName("Doit marquer le signalement comme non traité avec date de traitement null")
        void markAsHandled_False_Success() {
            sampleReport.setHandled(true);
            sampleReport.setHandleDateTime(LocalDateTime.now());
            when(repository.findById(100L)).thenReturn(Optional.of(sampleReport));
            when(repository.saveAndFlush(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Report unhandledReport = reportService.markAsHandled(100L, false);

            assertThat(unhandledReport.isHandled()).isFalse();
            assertThat(unhandledReport.getHandleDateTime()).isNull();
            verify(repository).saveAndFlush(sampleReport);
        }

        @Test
        @DisplayName("Doit renvoyer ResourceNotFoundException si le signalement n'existe pas")
        void markAsHandled_ThrowsException_WhenNotFound() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.markAsHandled(999L, true))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Signalement introuvable");
        }
    }

    @Nested
    @DisplayName("Tests de suppression (deleteById)")
    class DeleteTests {

        @Test
        @DisplayName("Le créateur doit pouvoir supprimer son signalement")
        void deleteById_Success_WhenCreator() {
            when(repository.findById(100L)).thenReturn(Optional.of(sampleReport));
            when(userService.currentUser()).thenReturn(creator);

            reportService.deleteById(100L);

            verify(repository).deleteById(100L);
        }

        @Test
        @DisplayName("Un gestionnaire doit pouvoir supprimer n'importe quel signalement")
        void deleteById_Success_WhenManager() {
            when(repository.findById(100L)).thenReturn(Optional.of(sampleReport));
            when(userService.currentUser()).thenReturn(managerUser);

            reportService.deleteById(100L);

            verify(repository).deleteById(100L);
        }

        @Test
        @DisplayName("Un autre utilisateur non manager doit être refusé avec 403")
        void deleteById_ThrowsAccessDenied_WhenNotCreatorNorManager() {
            when(repository.findById(100L)).thenReturn(Optional.of(sampleReport));
            when(userService.currentUser()).thenReturn(otherUser);

            assertThatThrownBy(() -> reportService.deleteById(100L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Vous n'êtes pas autorisé à supprimer ce signalement");

            verify(repository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Tests des requêtes de filtrage")
    class QueryFilterTests {

        @Test
        @DisplayName("findByMaterialId doit appeler le repository")
        void findByMaterialId_CallsRepository() {
            Page<Report> page = new PageImpl<>(List.of(sampleReport));
            Pageable pageable = PageRequest.of(0, 10);
            when(repository.findByMaterial_Id(10L, pageable)).thenReturn(page);

            Page<Report> result = reportService.findByMaterialId(10L, pageable);

            assertThat(result).hasSize(1);
            verify(repository).findByMaterial_Id(10L, pageable);
        }

        @Test
        @DisplayName("findByUserId doit appeler le repository")
        void findByUserId_CallsRepository() {
            Page<Report> page = new PageImpl<>(List.of(sampleReport));
            Pageable pageable = PageRequest.of(0, 10);
            when(repository.findByUser_Id(1L, pageable)).thenReturn(page);

            Page<Report> result = reportService.findByUserId(1L, pageable);

            assertThat(result).hasSize(1);
            verify(repository).findByUser_Id(1L, pageable);
        }

        @Test
        @DisplayName("findByHandled doit appeler le repository")
        void findByHandled_CallsRepository() {
            Page<Report> page = new PageImpl<>(List.of(sampleReport));
            Pageable pageable = PageRequest.of(0, 10);
            when(repository.findByHandled(false, pageable)).thenReturn(page);

            Page<Report> result = reportService.findByHandled(false, pageable);

            assertThat(result).hasSize(1);
            verify(repository).findByHandled(false, pageable);
        }
    }
}
