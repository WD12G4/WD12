package net.ent.etnc.services.impl;

import net.ent.etnc.models.Building;
import net.ent.etnc.repositories.BuildingRepository;
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
        @DisplayName("Doit lever ServiceException si le numéro est null ou négatif")
        void create_ThrowsException_WhenNumberNegativeOrNull() {
            sampleBuilding.setNumber(-5);
            when(repository.findByNumber(-5)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> buildingService.create(sampleBuilding))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("doit être positif");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit lever ServiceException si le numéro dépasse 999")
        void create_ThrowsException_WhenNumberGreaterThan999() {
            sampleBuilding.setNumber(1000);
            when(repository.findByNumber(1000)).thenReturn(Optional.empty());

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
            when(repository.existsById(1L)).thenReturn(true);
            when(repository.saveAndFlush(any(Building.class))).thenReturn(sampleBuilding);

            Building updated = buildingService.update(sampleBuilding);

            assertThat(updated).isNotNull();
            verify(repository).saveAndFlush(sampleBuilding);
        }

        @Test
        @DisplayName("Doit lever ServiceException si le numéro à mettre à jour est invalide")
        void update_ThrowsException_WhenInvalid() {
            sampleBuilding.setNumber(-1);

            assertThatThrownBy(() -> buildingService.update(sampleBuilding))
                    .isInstanceOf(ServiceException.class);
        }
    }
}
