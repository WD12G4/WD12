package net.ent.etnc.services.impl;

import net.ent.etnc.models.Building;
import net.ent.etnc.repositories.BuildingRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BuildingServiceImpl Unit Tests")
class BuildingServiceImplTest {

    @Mock
    private BuildingRepository repository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private BuildingServiceImpl buildingService;

    private Building sampleBuilding;

    @BeforeEach
    void setUp() {
        sampleBuilding = new Building();
        sampleBuilding.setId(1L);
        sampleBuilding.setNumber(101);
    }

    @Nested
    @DisplayName("Tests de création (create)")
    class CreateTests {

        @Test
        @DisplayName("Doit créer le bâtiment avec succès")
        void create_Success() {
            when(repository.findByNumber(101)).thenReturn(Optional.empty());
            when(repository.saveAndFlush(any(Building.class))).thenReturn(sampleBuilding);

            Building created = buildingService.create(sampleBuilding);

            assertThat(created).isNotNull();
            assertThat(created.getNumber()).isEqualTo(101);
            verify(repository).saveAndFlush(sampleBuilding);
        }

        @Test
        @DisplayName("Doit lever ServiceException si le numéro de bâtiment existe déjà")
        void create_ThrowsException_WhenNumberAlreadyExists() {
            when(repository.findByNumber(101)).thenReturn(Optional.of(sampleBuilding));

            assertThatThrownBy(() -> buildingService.create(sampleBuilding))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("existe déjà");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit lever ServiceException si le numéro est null")
        void create_ThrowsException_WhenNumberNull() {
            sampleBuilding.setNumber(null);

            assertThatThrownBy(() -> buildingService.create(sampleBuilding))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("doit être positif");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit lever ServiceException si le numéro est négatif")
        void create_ThrowsException_WhenNumberNegative() {
            sampleBuilding.setNumber(-5);

            assertThatThrownBy(() -> buildingService.create(sampleBuilding))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("doit être positif");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit lever ServiceException si le numéro dépasse 999")
        void create_ThrowsException_WhenNumberGreaterThan999() {
            sampleBuilding.setNumber(1000);

            assertThatThrownBy(() -> buildingService.create(sampleBuilding))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("inférieur à 999");
        }
    }

    @Nested
    @DisplayName("Tests de mise à jour (update)")
    class UpdateTests {

        @Test
        @DisplayName("Doit mettre à jour le bâtiment valide")
        void update_Success() {
            when(repository.findByNumber(101)).thenReturn(Optional.of(sampleBuilding));
            when(repository.existsById(1L)).thenReturn(true);
            when(repository.saveAndFlush(any(Building.class))).thenReturn(sampleBuilding);

            Building updated = buildingService.update(sampleBuilding);

            assertThat(updated).isNotNull();
            verify(repository).saveAndFlush(sampleBuilding);
        }

        @Test
        @DisplayName("Doit lever ServiceException si le numéro est déjà utilisé par un autre bâtiment")
        void update_ThrowsException_WhenNumberAlreadyUsedByAnother() {
            Building other = new Building();
            other.setId(2L);
            other.setNumber(101);

            when(repository.findByNumber(101)).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> buildingService.update(sampleBuilding))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("existe déjà");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit lever ServiceException si le numéro à mettre à jour est invalide")
        void update_ThrowsException_WhenInvalid() {
            sampleBuilding.setNumber(-1);

            assertThatThrownBy(() -> buildingService.update(sampleBuilding))
                    .isInstanceOf(ServiceException.class);
        }

        @Test
        @DisplayName("Doit lever ResourceNotFoundException si le bâtiment à mettre à jour n'existe pas")
        void update_ThrowsNotFound_WhenNotExisting() {
            when(repository.findByNumber(101)).thenReturn(Optional.empty());
            when(repository.existsById(1L)).thenReturn(false);

            assertThatThrownBy(() -> buildingService.update(sampleBuilding))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Tests de suppression (deleteById)")
    class DeleteTests {

        @Test
        @DisplayName("Doit supprimer le bâtiment lorsqu'il n'a pas de salles")
        void deleteById_Success() {
            when(repository.existsById(1L)).thenReturn(true);
            when(roomRepository.existsByBuildingId(1L)).thenReturn(false);

            buildingService.deleteById(1L);

            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("Doit lever ResourceNotFoundException si le bâtiment n'existe pas")
        void deleteById_ThrowsNotFound_WhenNotExisting() {
            when(repository.existsById(1L)).thenReturn(false);

            assertThatThrownBy(() -> buildingService.deleteById(1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Bâtiment introuvable avec l'id : 1");

            verify(repository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("Doit lever ServiceException si le bâtiment contient des salles")
        void deleteById_ThrowsException_WhenContainsRooms() {
            when(repository.existsById(1L)).thenReturn(true);
            when(roomRepository.existsByBuildingId(1L)).thenReturn(true);

            assertThatThrownBy(() -> buildingService.deleteById(1L))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("Impossible de supprimer le bâtiment : il contient encore des salles.");

            verify(repository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("Tests de recherche par numéro (findByNumber)")
    class FindByNumberTests {

        @Test
        @DisplayName("Doit trouver un bâtiment par son numéro")
        void findByNumber_Success() {
            when(repository.findByNumber(101)).thenReturn(Optional.of(sampleBuilding));

            Optional<Building> found = buildingService.findByNumber(101);

            assertThat(found).isPresent().contains(sampleBuilding);
        }
    }
}
