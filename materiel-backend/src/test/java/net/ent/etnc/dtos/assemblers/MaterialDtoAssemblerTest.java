package net.ent.etnc.dtos.assemblers;

import net.ent.etnc.dtos.building.BuildingResponse;
import net.ent.etnc.dtos.material.MaterialRequest;
import net.ent.etnc.dtos.material.MaterialResponse;
import net.ent.etnc.dtos.room.RoomResponse;
import net.ent.etnc.models.Building;
import net.ent.etnc.models.Material;
import net.ent.etnc.models.Room;
import net.ent.etnc.models.enums.EMaterialType;
import net.ent.etnc.services.RoomService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaterialDtoAssembler Unit Tests")
class MaterialDtoAssemblerTest {

    @Mock
    private RoomDtoAssembler roomAssembler;

    @Mock
    private RoomService roomService;

    @InjectMocks
    private MaterialDtoAssembler materialAssembler;

    private Material sampleMaterial;
    private Room sampleRoom;
    private RoomResponse sampleRoomResponse;

    @BeforeEach
    void setUp() {
        sampleRoom = new Room();
        sampleRoom.setId(5L);
        sampleRoom.setNumber(101);

        sampleRoomResponse = new RoomResponse(5L, 0L, 101, new BuildingResponse(1L, 0L, 1));

        sampleMaterial = new Material();
        sampleMaterial.setId(10L);
        sampleMaterial.setVersion(0L);
        sampleMaterial.setSerialNumber("SN-9999");
        sampleMaterial.setMaterialType(EMaterialType.UC);
        sampleMaterial.setEndAvailabilityDate(LocalDate.now().plusYears(1));
        sampleMaterial.setRoom(sampleRoom);
        sampleMaterial.setArchived(false);
    }

    @Nested
    @DisplayName("Tests toDto")
    class ToDtoTests {

        @Test
        @DisplayName("Doit retourner null quand l'entité est null")
        void toDto_WhenEntityIsNull_ReturnsNull() {
            assertThat(materialAssembler.toDto(null)).isNull();
        }

        @Test
        @DisplayName("Doit convertir correctement le matériel en DTO")
        void toDto_Success() {
            when(roomAssembler.toDto(sampleRoom)).thenReturn(sampleRoomResponse);

            MaterialResponse response = materialAssembler.toDto(sampleMaterial);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.version()).isEqualTo(0L);
            assertThat(response.serialNumber()).isEqualTo("SN-9999");
            assertThat(response.materialType()).isEqualTo(EMaterialType.UC);
            assertThat(response.room()).isEqualTo(sampleRoomResponse);
            assertThat(response.archived()).isFalse();
        }

        @Test
        @DisplayName("Doit gérer un matériel sans salle")
        void toDto_WhenRoomIsNull_ReturnsMaterialWithNullRoom() {
            sampleMaterial.setRoom(null);

            MaterialResponse response = materialAssembler.toDto(sampleMaterial);

            assertThat(response).isNotNull();
            assertThat(response.room()).isNull();
        }

        @Test
        @DisplayName("Doit gérer un matériel sans date de fin de disponibilité")
        void toDto_WhenEndAvailabilityDateIsNull_ReturnsMaterialWithNullDate() {
            sampleMaterial.setEndAvailabilityDate(null);
            when(roomAssembler.toDto(sampleRoom)).thenReturn(sampleRoomResponse);

            MaterialResponse response = materialAssembler.toDto(sampleMaterial);

            assertThat(response).isNotNull();
            assertThat(response.endAvailabilityDate()).isNull();
        }
    }

    @Nested
    @DisplayName("Tests toEntity")
    class ToEntityTests {

        @Test
        @DisplayName("Doit retourner null quand la requête est null")
        void toEntity_WhenRequestIsNull_ReturnsNull() {
            assertThat(materialAssembler.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("Doit convertir la requête avec roomId en entité")
        void toEntity_WithRoomId_Success() {
            when(roomService.findById(5L)).thenReturn(Optional.of(sampleRoom));

            LocalDate date = LocalDate.now().plusYears(1);
            MaterialRequest request = new MaterialRequest("SN-8888", EMaterialType.SCREEN, date, 5L, false);

            Material entity = materialAssembler.toEntity(request);

            assertThat(entity).isNotNull();
            assertThat(entity.getSerialNumber()).isEqualTo("SN-8888");
            assertThat(entity.getMaterialType()).isEqualTo(EMaterialType.SCREEN);
            assertThat(entity.getEndAvailabilityDate()).isEqualTo(date);
            assertThat(entity.getRoom()).isEqualTo(sampleRoom);
            assertThat(entity.isArchived()).isFalse();
        }

        @Test
        @DisplayName("Doit convertir la requête sans roomId en entité sans salle")
        void toEntity_WithoutRoomId_Success() {
            LocalDate date = LocalDate.now().plusYears(1);
            MaterialRequest request = new MaterialRequest("SN-8888", EMaterialType.PERIPHERAL, date, null, true);

            Material entity = materialAssembler.toEntity(request);

            assertThat(entity).isNotNull();
            assertThat(entity.getRoom()).isNull();
            assertThat(entity.isArchived()).isTrue();
        }

        @Test
        @DisplayName("Doit convertir la requête sans date de fin de disponibilité en entité avec date null")
        void toEntity_WithNullDate_Success() {
            MaterialRequest request = new MaterialRequest("SN-8888", EMaterialType.PERIPHERAL, null, null, false);

            Material entity = materialAssembler.toEntity(request);

            assertThat(entity).isNotNull();
            assertThat(entity.getEndAvailabilityDate()).isNull();
        }

        @Test
        @DisplayName("Doit lever ResourceNotFoundException si la salle est introuvable")
        void toEntity_WhenRoomNotFound_ThrowsException() {
            when(roomService.findById(999L)).thenReturn(Optional.empty());

            MaterialRequest request = new MaterialRequest("SN-8888", EMaterialType.UC, LocalDate.now(), 999L, false);

            assertThatThrownBy(() -> materialAssembler.toEntity(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Salle introuvable avec l'id : 999");
        }
    }

    @Nested
    @DisplayName("Tests updateEntity")
    class UpdateEntityTests {

        @Test
        @DisplayName("Doit mettre à jour les champs du matériel")
        void updateEntity_Success() {
            Room newRoom = new Room();
            newRoom.setId(8L);
            when(roomService.findById(8L)).thenReturn(Optional.of(newRoom));

            LocalDate date = LocalDate.now().plusYears(2);
            MaterialRequest request = new MaterialRequest("SN-UPDATED", EMaterialType.PROJECTOR, date, 8L, true);

            materialAssembler.updateEntity(sampleMaterial, request);

            assertThat(sampleMaterial.getSerialNumber()).isEqualTo("SN-UPDATED");
            assertThat(sampleMaterial.getMaterialType()).isEqualTo(EMaterialType.PROJECTOR);
            assertThat(sampleMaterial.getEndAvailabilityDate()).isEqualTo(date);
            assertThat(sampleMaterial.getRoom()).isEqualTo(newRoom);
            assertThat(sampleMaterial.isArchived()).isTrue();
        }

        @Test
        @DisplayName("Doit dissocier la salle si roomId est null")
        void updateEntity_WhenRoomIdNull_UnassignsRoom() {
            MaterialRequest request = new MaterialRequest("SN-9999", EMaterialType.UC, LocalDate.now().plusYears(1), null, false);

            materialAssembler.updateEntity(sampleMaterial, request);

            assertThat(sampleMaterial.getRoom()).isNull();
        }

        @Test
        @DisplayName("Doit mettre à jour la date de fin de disponibilité à null si spécifiée à null")
        void updateEntity_WithNullDate_SetsNullDate() {
            MaterialRequest request = new MaterialRequest("SN-UPDATED", EMaterialType.PROJECTOR, null, null, false);

            materialAssembler.updateEntity(sampleMaterial, request);

            assertThat(sampleMaterial.getEndAvailabilityDate()).isNull();
        }

        @Test
        @DisplayName("Doit ignorer sans lever d'erreur si entité ou requête est null")
        void updateEntity_WhenNull_DoesNotThrow() {
            materialAssembler.updateEntity(null, new MaterialRequest("SN", EMaterialType.UC, LocalDate.now(), null, false));
            materialAssembler.updateEntity(sampleMaterial, null);

            assertThat(sampleMaterial.getSerialNumber()).isEqualTo("SN-9999");
        }
    }

    @Nested
    @DisplayName("Tests toDtoList")
    class ToDtoListTests {

        @Test
        @DisplayName("Doit convertir une collection de matériels en liste de DTOs")
        void toDtoList_Success() {
            when(roomAssembler.toDto(sampleRoom)).thenReturn(sampleRoomResponse);

            List<MaterialResponse> list = materialAssembler.toDtoList(List.of(sampleMaterial));

            assertThat(list).hasSize(1);
            assertThat(list.get(0).serialNumber()).isEqualTo("SN-9999");
        }

        @Test
        @DisplayName("Doit retourner une liste vide si collection est null")
        void toDtoList_WhenNull_ReturnsEmpty() {
            assertThat(materialAssembler.toDtoList(null)).isEmpty();
        }
    }
}
