package net.ent.etnc.services.impl;

import net.ent.etnc.models.Building;
import net.ent.etnc.models.Room;
import net.ent.etnc.repositories.MaterialRepository;
import net.ent.etnc.repositories.RoomRepository;
import net.ent.etnc.services.BuildingService;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomServiceImpl Unit Tests")
class RoomServiceImplTest {

    @Mock
    private RoomRepository repository;

    @Mock
    private BuildingService buildingService;

    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private RoomServiceImpl roomService;

    private Building sampleBuilding;
    private Room sampleRoom;

    @BeforeEach
    void setUp() {
        sampleBuilding = new Building();
        sampleBuilding.setId(1L);
        sampleBuilding.setNumber(101);

        sampleRoom = new Room();
        sampleRoom.setId(10L);
        sampleRoom.setNumber(12);
        sampleRoom.setBuilding(sampleBuilding);

        lenient().when(buildingService.findById(1L)).thenReturn(Optional.of(sampleBuilding));
    }

    @Nested
    @DisplayName("Tests de création (create)")
    class CreateTests {

        @Test
        @DisplayName("Doit créer la salle avec succès")
        void create_Success() {
            when(repository.findByNumberAndBuilding(12, sampleBuilding)).thenReturn(Optional.empty());
            when(repository.saveAndFlush(any(Room.class))).thenReturn(sampleRoom);

            Room created = roomService.create(sampleRoom);

            assertThat(created).isNotNull();
            assertThat(created.getNumber()).isEqualTo(12);
            assertThat(created.getBuilding()).isEqualTo(sampleBuilding);
            verify(buildingService).findById(1L);
            verify(repository).saveAndFlush(sampleRoom);
        }

        @Test
        @DisplayName("Doit lever ServiceException si le bâtiment est null")
        void create_ThrowsException_WhenBuildingNull() {
            sampleRoom.setBuilding(null);

            assertThatThrownBy(() -> roomService.create(sampleRoom))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("Le bâtiment doit être renseigné");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit lever ServiceException si l'identifiant du bâtiment est null")
        void create_ThrowsException_WhenBuildingIdNull() {
            Building noIdBuilding = new Building();
            sampleRoom.setBuilding(noIdBuilding);

            assertThatThrownBy(() -> roomService.create(sampleRoom))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("Le bâtiment doit être renseigné");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit lever ResourceNotFoundException si le bâtiment n'existe pas en base")
        void create_ThrowsException_WhenBuildingNotFound() {
            when(buildingService.findById(999L)).thenReturn(Optional.empty());
            Building nonExisting = new Building();
            nonExisting.setId(999L);
            sampleRoom.setBuilding(nonExisting);

            assertThatThrownBy(() -> roomService.create(sampleRoom))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Bâtiment introuvable avec l'id : 999");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit lever ServiceException si le numéro de salle est null")
        void create_ThrowsException_WhenNumberNull() {
            sampleRoom.setNumber(null);

            assertThatThrownBy(() -> roomService.create(sampleRoom))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("doit être positif");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit lever ServiceException si le numéro de salle est négatif")
        void create_ThrowsException_WhenNumberNegative() {
            sampleRoom.setNumber(-1);

            assertThatThrownBy(() -> roomService.create(sampleRoom))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("doit être positif");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit lever ServiceException si le numéro de salle dépasse 999")
        void create_ThrowsException_WhenNumberGreaterThan999() {
            sampleRoom.setNumber(1000);

            assertThatThrownBy(() -> roomService.create(sampleRoom))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("inférieur à 999");
        }

        @Test
        @DisplayName("Doit lever ServiceException si la salle existe déjà dans ce bâtiment")
        void create_ThrowsException_WhenRoomAlreadyExistsInBuilding() {
            when(repository.findByNumberAndBuilding(12, sampleBuilding)).thenReturn(Optional.of(sampleRoom));

            assertThatThrownBy(() -> roomService.create(sampleRoom))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("existe déjà dans le bâtiment");

            verify(repository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("Tests de mise à jour (update)")
    class UpdateTests {

        @Test
        @DisplayName("Doit mettre à jour la salle valide")
        void update_Success() {
            when(repository.findByNumberAndBuilding(12, sampleBuilding)).thenReturn(Optional.of(sampleRoom));
            when(repository.existsById(10L)).thenReturn(true);
            when(repository.saveAndFlush(any(Room.class))).thenReturn(sampleRoom);

            Room updated = roomService.update(sampleRoom);

            assertThat(updated).isNotNull();
            verify(buildingService).findById(1L);
            verify(repository).saveAndFlush(sampleRoom);
        }

        @Test
        @DisplayName("Doit lever ServiceException si le numéro de salle est déjà utilisé dans ce bâtiment")
        void update_ThrowsException_WhenRoomNumberAlreadyUsedInBuilding() {
            Room other = new Room();
            other.setId(20L);
            other.setNumber(12);
            other.setBuilding(sampleBuilding);

            when(repository.findByNumberAndBuilding(12, sampleBuilding)).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> roomService.update(sampleRoom))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("existe déjà dans le bâtiment");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit lever ServiceException si la validation échoue lors de l'update")
        void update_ThrowsException_WhenInvalid() {
            sampleRoom.setBuilding(null);

            assertThatThrownBy(() -> roomService.update(sampleRoom))
                    .isInstanceOf(ServiceException.class);
        }

        @Test
        @DisplayName("Doit lever ResourceNotFoundException si la salle n'existe pas en BDD lors de l'update")
        void update_ThrowsNotFound_WhenNotExisting() {
            when(repository.findByNumberAndBuilding(12, sampleBuilding)).thenReturn(Optional.empty());
            when(repository.existsById(10L)).thenReturn(false);

            assertThatThrownBy(() -> roomService.update(sampleRoom))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Tests de suppression (deleteById)")
    class DeleteTests {

        @Test
        @DisplayName("Doit supprimer la salle lorsqu'elle ne contient pas de matériel")
        void deleteById_Success() {
            when(repository.existsById(10L)).thenReturn(true);
            when(materialRepository.existsByRoomId(10L)).thenReturn(false);

            roomService.deleteById(10L);

            verify(repository).deleteById(10L);
        }

        @Test
        @DisplayName("Doit lever ResourceNotFoundException si la salle n'existe pas")
        void deleteById_ThrowsNotFound_WhenNotExisting() {
            when(repository.existsById(10L)).thenReturn(false);

            assertThatThrownBy(() -> roomService.deleteById(10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Salle introuvable avec l'id : 10");

            verify(repository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("Doit lever ServiceException si la salle contient du matériel")
        void deleteById_ThrowsException_WhenContainsMaterial() {
            when(repository.existsById(10L)).thenReturn(true);
            when(materialRepository.existsByRoomId(10L)).thenReturn(true);

            assertThatThrownBy(() -> roomService.deleteById(10L))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("Impossible de supprimer la salle : elle contient encore du matériel.");

            verify(repository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("Tests de recherche par numéro et bâtiment")
    class FindByNumberAndBuildingTests {

        @Test
        @DisplayName("Doit trouver la salle par numéro et bâtiment")
        void findByNumberAndBuilding_Success() {
            when(repository.findByNumberAndBuilding(12, sampleBuilding)).thenReturn(Optional.of(sampleRoom));

            Optional<Room> found = roomService.findByNumberAndBuilding(12, sampleBuilding);

            assertThat(found).isPresent().contains(sampleRoom);
        }
    }
}
