package net.ent.etnc.dtos.assemblers;

import net.ent.etnc.dtos.building.BuildingResponse;
import net.ent.etnc.dtos.room.RoomRequest;
import net.ent.etnc.dtos.room.RoomResponse;
import net.ent.etnc.models.Building;
import net.ent.etnc.models.Room;
import net.ent.etnc.services.BuildingService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomDtoAssembler Unit Tests")
class RoomDtoAssemblerTest {

    @Mock
    private BuildingDtoAssembler buildingAssembler;

    @Mock
    private BuildingService buildingService;

    @InjectMocks
    private RoomDtoAssembler roomAssembler;

    private Room sampleRoom;
    private Building sampleBuilding;
    private BuildingResponse sampleBuildingResponse;

    @BeforeEach
    void setUp() {
        sampleBuilding = new Building();
        sampleBuilding.setId(10L);
        sampleBuilding.setVersion(0L);
        sampleBuilding.setNumber(1);

        sampleBuildingResponse = new BuildingResponse(10L, 0L, 1);

        sampleRoom = new Room();
        sampleRoom.setId(1L);
        sampleRoom.setVersion(0L);
        sampleRoom.setNumber(101);
        sampleRoom.setBuilding(sampleBuilding);
    }

    @Nested
    @DisplayName("Tests toDto")
    class ToDtoTests {

        @Test
        @DisplayName("Doit retourner null quand l'entité est null")
        void toDto_WhenEntityIsNull_ReturnsNull() {
            assertThat(roomAssembler.toDto(null)).isNull();
        }

        @Test
        @DisplayName("Doit convertir correctement la salle en DTO")
        void toDto_Success() {
            when(buildingAssembler.toDto(sampleBuilding)).thenReturn(sampleBuildingResponse);

            RoomResponse response = roomAssembler.toDto(sampleRoom);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.version()).isEqualTo(0L);
            assertThat(response.number()).isEqualTo(101);
            assertThat(response.building()).isEqualTo(sampleBuildingResponse);
        }

        @Test
        @DisplayName("Doit gérer une salle sans bâtiment")
        void toDto_WhenBuildingIsNull_ReturnsRoomWithNullBuilding() {
            sampleRoom.setBuilding(null);
            when(buildingAssembler.toDto(null)).thenReturn(null);

            RoomResponse response = roomAssembler.toDto(sampleRoom);

            assertThat(response).isNotNull();
            assertThat(response.building()).isNull();
        }
    }

    @Nested
    @DisplayName("Tests toEntity")
    class ToEntityTests {

        @Test
        @DisplayName("Doit retourner null quand la requête est null")
        void toEntity_WhenRequestIsNull_ReturnsNull() {
            assertThat(roomAssembler.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("Doit convertir la requête en entité avec son bâtiment")
        void toEntity_Success() {
            when(buildingService.findById(10L)).thenReturn(Optional.of(sampleBuilding));

            RoomRequest request = new RoomRequest(10L, 202);
            Room entity = roomAssembler.toEntity(request);

            assertThat(entity).isNotNull();
            assertThat(entity.getNumber()).isEqualTo(202);
            assertThat(entity.getBuilding()).isEqualTo(sampleBuilding);
        }

        @Test
        @DisplayName("Doit lever ResourceNotFoundException si le bâtiment est introuvable")
        void toEntity_WhenBuildingNotFound_ThrowsResourceNotFoundException() {
            when(buildingService.findById(999L)).thenReturn(Optional.empty());

            RoomRequest request = new RoomRequest(999L, 202);

            assertThatThrownBy(() -> roomAssembler.toEntity(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Bâtiment introuvable avec l'id : 999");
        }
    }

    @Nested
    @DisplayName("Tests updateEntity")
    class UpdateEntityTests {

        @Test
        @DisplayName("Doit mettre à jour le numéro et le bâtiment")
        void updateEntity_Success() {
            Building newBuilding = new Building();
            newBuilding.setId(20L);
            newBuilding.setNumber(2);

            when(buildingService.findById(20L)).thenReturn(Optional.of(newBuilding));

            RoomRequest request = new RoomRequest(20L, 205);
            roomAssembler.updateEntity(sampleRoom, request);

            assertThat(sampleRoom.getNumber()).isEqualTo(205);
            assertThat(sampleRoom.getBuilding()).isEqualTo(newBuilding);
        }

        @Test
        @DisplayName("Doit mettre à jour le numéro sans altérer le bâtiment si buildingId est null")
        void updateEntity_WhenBuildingIdNull_KeepsExistingBuilding() {
            RoomRequest request = new RoomRequest(null, 205);
            roomAssembler.updateEntity(sampleRoom, request);

            assertThat(sampleRoom.getNumber()).isEqualTo(205);
            assertThat(sampleRoom.getBuilding()).isEqualTo(sampleBuilding);
        }

        @Test
        @DisplayName("Doit ignorer sans lever d'erreur si entité ou requête est null")
        void updateEntity_WhenNull_DoesNotThrow() {
            roomAssembler.updateEntity(null, new RoomRequest(10L, 205));
            roomAssembler.updateEntity(sampleRoom, null);

            assertThat(sampleRoom.getNumber()).isEqualTo(101);
        }
    }

    @Nested
    @DisplayName("Tests toDtoList")
    class ToDtoListTests {

        @Test
        @DisplayName("Doit convertir une liste de salles en liste de DTOs")
        void toDtoList_Success() {
            when(buildingAssembler.toDto(sampleBuilding)).thenReturn(sampleBuildingResponse);

            List<RoomResponse> list = roomAssembler.toDtoList(List.of(sampleRoom));

            assertThat(list).hasSize(1);
            assertThat(list.get(0).number()).isEqualTo(101);
        }

        @Test
        @DisplayName("Doit renvoyer une liste vide si la collection est null")
        void toDtoList_WhenNull_ReturnsEmpty() {
            List<RoomResponse> list = roomAssembler.toDtoList(null);

            assertThat(list).isEmpty();
        }
    }
}
