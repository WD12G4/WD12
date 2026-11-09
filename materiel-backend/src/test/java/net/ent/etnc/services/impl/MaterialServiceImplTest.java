package net.ent.etnc.services.impl;

import net.ent.etnc.models.Material;
import net.ent.etnc.models.Room;
import net.ent.etnc.models.enums.EMaterialType;
import net.ent.etnc.repositories.MaterialRepository;
import net.ent.etnc.repositories.RoomRepository;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaterialServiceImpl Unit Tests")
class MaterialServiceImplTest {

    @Mock
    private MaterialRepository repository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private MaterialServiceImpl materialService;

    private Material sampleMaterial;

    @BeforeEach
    void setUp() {
        sampleMaterial = new Material();
        sampleMaterial.setId(1L);
        sampleMaterial.setSerialNumber("SN-12345");
        sampleMaterial.setMaterialType(EMaterialType.UC);
        sampleMaterial.setEndAvailabilityDate(LocalDate.now().plusYears(2));
        sampleMaterial.setArchived(false);
    }

    @Nested
    @DisplayName("Tests de création (create)")
    class CreateTests {

        @Test
        @DisplayName("Doit créer et retourner le matériel avec succès")
        void create_Success() {
            when(repository.existsBySerialNumber("SN-12345")).thenReturn(false);
            when(repository.saveAndFlush(any(Material.class))).thenReturn(sampleMaterial);

            Material created = materialService.create(sampleMaterial);

            assertThat(created).isNotNull();
            assertThat(created.getSerialNumber()).isEqualTo("SN-12345");
            verify(repository).saveAndFlush(sampleMaterial);
        }

        @Test
        @DisplayName("Doit échouer si le numéro de série existe déjà")
        void create_ThrowsException_WhenSerialNumberExists() {
            when(repository.existsBySerialNumber("SN-12345")).thenReturn(true);

            assertThatThrownBy(() -> materialService.create(sampleMaterial))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("existe déjà");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit échouer si le numéro de série est null")
        void create_ThrowsException_WhenSerialNumberNull() {
            sampleMaterial.setSerialNumber(null);

            assertThatThrownBy(() -> materialService.create(sampleMaterial))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("numéro de série doit être renseigné");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit échouer si la date de fin de disponibilité est dans le passé pour un matériel non archivé")
        void create_ThrowsException_WhenDateInPastAndNotArchived() {
            sampleMaterial.setEndAvailabilityDate(LocalDate.now().minusDays(1));
            when(repository.existsBySerialNumber("SN-12345")).thenReturn(false);

            assertThatThrownBy(() -> materialService.create(sampleMaterial))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("La date de disponibilité ne peut pas être avant le jour en cours");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit échouer si le matériel est archivé mais associé à une salle")
        void create_ThrowsException_WhenArchivedAndAssignedToRoom() {
            sampleMaterial.setArchived(true);
            sampleMaterial.setRoom(new Room());
            when(repository.existsBySerialNumber("SN-12345")).thenReturn(false);

            assertThatThrownBy(() -> materialService.create(sampleMaterial))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("Le matériel ne peut pas être affecté à une salle s'il est archivé");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit échouer si le numéro de série dépasse 100 caractères")
        void create_ThrowsException_WhenSerialNumberTooLong() {
            sampleMaterial.setSerialNumber("A".repeat(101));
            when(repository.existsBySerialNumber(any())).thenReturn(false);

            assertThatThrownBy(() -> materialService.create(sampleMaterial))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("inférieur à 100 caractères");
        }

        @Test
        @DisplayName("Doit échouer si le type de matériel est nul")
        void create_ThrowsException_WhenMaterialTypeNull() {
            sampleMaterial.setMaterialType(null);
            when(repository.existsBySerialNumber("SN-12345")).thenReturn(false);

            assertThatThrownBy(() -> materialService.create(sampleMaterial))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("type de matériel doit être renseigné");
        }
    }

    @Nested
    @DisplayName("Tests d'affectation de salle (assignRoom)")
    class AssignRoomTests {

        @Test
        @DisplayName("Doit affecter le matériel à une salle existante")
        void assignRoom_Success() {
            Room room = new Room();
            room.setId(10L);

            when(repository.findById(1L)).thenReturn(Optional.of(sampleMaterial));
            when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
            when(repository.saveAndFlush(any(Material.class))).thenReturn(sampleMaterial);

            Material result = materialService.assignRoom(1L, 10L);

            assertThat(result.getRoom()).isEqualTo(room);
            verify(repository).saveAndFlush(sampleMaterial);
        }

        @Test
        @DisplayName("Doit désaffecter le matériel quand roomId est null")
        void assignRoom_Unassign_Success() {
            Room room = new Room();
            sampleMaterial.setRoom(room);

            when(repository.findById(1L)).thenReturn(Optional.of(sampleMaterial));
            when(repository.saveAndFlush(any(Material.class))).thenReturn(sampleMaterial);

            Material result = materialService.assignRoom(1L, null);

            assertThat(result.getRoom()).isNull();
            verify(repository).saveAndFlush(sampleMaterial);
        }

        @Test
        @DisplayName("Doit échouer si le matériel n'existe pas lors de l'affectation")
        void assignRoom_ThrowsNotFound_WhenMaterialDoesNotExist() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> materialService.assignRoom(99L, 10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Matériel introuvable");
        }

        @Test
        @DisplayName("Doit échouer si la salle n'existe pas")
        void assignRoom_ThrowsNotFound_WhenRoomDoesNotExist() {
            when(repository.findById(1L)).thenReturn(Optional.of(sampleMaterial));
            when(roomRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> materialService.assignRoom(1L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Salle introuvable");
        }

        @Test
        @DisplayName("Doit échouer si le matériel est archivé")
        void assignRoom_ThrowsException_WhenMaterialArchived() {
            sampleMaterial.setArchived(true);
            when(repository.findById(1L)).thenReturn(Optional.of(sampleMaterial));

            assertThatThrownBy(() -> materialService.assignRoom(1L, 10L))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("s'il est archivé");
        }
    }

    @Nested
    @DisplayName("Tests d'archivage (archiveById / deleteById)")
    class ArchiveTests {

        @Test
        @DisplayName("Doit archiver le matériel existant et dissocier sa salle")
        void archiveById_Success() {
            Room assignedRoom = new Room();
            sampleMaterial.setRoom(assignedRoom);

            when(repository.findById(1L)).thenReturn(Optional.of(sampleMaterial));
            when(repository.saveAndFlush(any(Material.class))).thenReturn(sampleMaterial);

            materialService.archiveById(1L);

            assertThat(sampleMaterial.isArchived()).isTrue();
            assertThat(sampleMaterial.getRoom()).isNull();
            verify(repository).saveAndFlush(sampleMaterial);
        }

        @Test
        @DisplayName("Doit lancer une ResourceNotFoundException si le matériel à archiver n'existe pas")
        void archiveById_ThrowsNotFound_WhenNotExisting() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> materialService.archiveById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Matériel introuvable");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("deleteById doit rediriger vers l'archivage")
        void deleteById_CallsArchive() {
            when(repository.findById(1L)).thenReturn(Optional.of(sampleMaterial));
            when(repository.saveAndFlush(any(Material.class))).thenReturn(sampleMaterial);

            materialService.deleteById(1L);

            assertThat(sampleMaterial.isArchived()).isTrue();
            verify(repository).saveAndFlush(sampleMaterial);
        }
    }

    @Nested
    @DisplayName("Tests de mise à jour (update)")
    class UpdateTests {

        @Test
        @DisplayName("Doit mettre à jour le matériel valide")
        void update_Success() {
            when(repository.existsById(1L)).thenReturn(true);
            when(repository.saveAndFlush(any(Material.class))).thenReturn(sampleMaterial);

            Material updated = materialService.update(sampleMaterial);

            assertThat(updated).isNotNull();
            verify(repository).saveAndFlush(sampleMaterial);
        }

        @Test
        @DisplayName("Doit échouer si le matériel à mettre à jour n'existe pas")
        void update_ThrowsNotFound_WhenNotExisting() {
            when(repository.existsById(1L)).thenReturn(false);

            assertThatThrownBy(() -> materialService.update(sampleMaterial))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Doit échouer si la validation échoue lors de l'update")
        void update_ThrowsException_WhenInvalid() {
            sampleMaterial.setSerialNumber(null);

            assertThatThrownBy(() -> materialService.update(sampleMaterial))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("numéro de série doit être renseigné");
        }
    }
}
