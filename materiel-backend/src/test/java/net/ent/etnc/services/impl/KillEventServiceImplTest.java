package net.ent.etnc.services.impl;

import net.ent.etnc.models.KillEvent;
import net.ent.etnc.models.Material;
import net.ent.etnc.models.enums.EMaterialType;
import net.ent.etnc.repositories.KillEventRepository;
import net.ent.etnc.repositories.MaterialRepository;
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

import java.time.LocalDateTime;
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

    @InjectMocks
    private KillEventServiceImpl killEventService;

    private Material sampleUc;
    private KillEvent sampleKillEvent;

    @BeforeEach
    void setUp() {
        sampleUc = new Material();
        sampleUc.setId(1L);
        sampleUc.setSerialNumber("UC-12345");
        sampleUc.setMaterialType(EMaterialType.UC);

        sampleKillEvent = new KillEvent();
        sampleKillEvent.setId(10L);
        sampleKillEvent.setProcessName("notepad.exe");
        sampleKillEvent.setUc(sampleUc);
        sampleKillEvent.setKillDateTime(LocalDateTime.now().minusMinutes(5));
        sampleKillEvent.setResult(true);
    }

    @Nested
    @DisplayName("Tests de création (create)")
    class CreateTests {

        @Test
        @DisplayName("Doit créer et retourner le KillEvent avec succès")
        void create_Success() {
            when(materialRepository.findById(1L)).thenReturn(Optional.of(sampleUc));
            when(repository.saveAndFlush(any(KillEvent.class))).thenReturn(sampleKillEvent);

            KillEvent created = killEventService.create(sampleKillEvent);

            assertThat(created).isNotNull();
            assertThat(created.getProcessName()).isEqualTo("notepad.exe");
            assertThat(created.getUc()).isEqualTo(sampleUc);
            assertThat(created.getResult()).isTrue();
            verify(repository).saveAndFlush(sampleKillEvent);
        }

        @Test
        @DisplayName("Doit échouer si le nom du processus est null")
        void create_ThrowsException_WhenProcessNameNull() {
            sampleKillEvent.setProcessName(null);

            assertThatThrownBy(() -> killEventService.create(sampleKillEvent))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("nom du processus doit être renseigné");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit échouer si le nom du processus est vide")
        void create_ThrowsException_WhenProcessNameBlank() {
            sampleKillEvent.setProcessName("   ");

            assertThatThrownBy(() -> killEventService.create(sampleKillEvent))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("nom du processus doit être renseigné");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit échouer si le nom du processus dépasse 100 caractères")
        void create_ThrowsException_WhenProcessNameTooLong() {
            sampleKillEvent.setProcessName("a".repeat(101));

            assertThatThrownBy(() -> killEventService.create(sampleKillEvent))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("inférieur à 100 caractères");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit échouer si l'UC est null")
        void create_ThrowsException_WhenUcNull() {
            sampleKillEvent.setUc(null);

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

            assertThatThrownBy(() -> killEventService.create(sampleKillEvent))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("L'UC doit être renseignée");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit renvoyer ResourceNotFoundException si l'UC n'existe pas en BDD")
        void create_ThrowsException_WhenUcNotFound() {
            when(materialRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> killEventService.create(sampleKillEvent))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("UC non trouvée avec l'id : 1");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit échouer si le matériel n'est pas une UC")
        void create_ThrowsException_WhenMaterialIsNotUc() {
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
            when(materialRepository.findById(1L)).thenReturn(Optional.of(sampleUc));
            sampleKillEvent.setKillDateTime(LocalDateTime.now().plusDays(1));

            assertThatThrownBy(() -> killEventService.create(sampleKillEvent))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("ne peut pas être après le jour/l'heure actuelle");

            verify(repository, never()).saveAndFlush(any());
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
