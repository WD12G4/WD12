package net.ent.etnc.dtos.assemblers;

import net.ent.etnc.dtos.building.BuildingRequest;
import net.ent.etnc.dtos.building.BuildingResponse;
import net.ent.etnc.models.Building;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BuildingDtoAssembler Unit Tests")
class BuildingDtoAssemblerTest {

    private BuildingDtoAssembler assembler;
    private Building sampleBuilding;

    @BeforeEach
    void setUp() {
        assembler = new BuildingDtoAssembler();
        sampleBuilding = new Building();
        sampleBuilding.setId(1L);
        sampleBuilding.setVersion(0L);
        sampleBuilding.setNumber(101);
    }

    @Nested
    @DisplayName("Tests toDto")
    class ToDtoTests {

        @Test
        @DisplayName("Doit retourner null quand l'entité est null")
        void toDto_WhenEntityIsNull_ReturnsNull() {
            assertThat(assembler.toDto(null)).isNull();
        }

        @Test
        @DisplayName("Doit convertir correctement l'entité en DTO")
        void toDto_Success() {
            BuildingResponse response = assembler.toDto(sampleBuilding);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.version()).isEqualTo(0L);
            assertThat(response.number()).isEqualTo(101);
        }
    }

    @Nested
    @DisplayName("Tests toEntity")
    class ToEntityTests {

        @Test
        @DisplayName("Doit retourner null quand la requête est null")
        void toEntity_WhenRequestIsNull_ReturnsNull() {
            assertThat(assembler.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("Doit convertir correctement la requête en entité")
        void toEntity_Success() {
            BuildingRequest request = new BuildingRequest(202);

            Building entity = assembler.toEntity(request);

            assertThat(entity).isNotNull();
            assertThat(entity.getNumber()).isEqualTo(202);
        }
    }

    @Nested
    @DisplayName("Tests updateEntity")
    class UpdateEntityTests {

        @Test
        @DisplayName("Doit mettre à jour le numéro de l'entité")
        void updateEntity_Success() {
            BuildingRequest request = new BuildingRequest(303);

            assembler.updateEntity(sampleBuilding, request);

            assertThat(sampleBuilding.getNumber()).isEqualTo(303);
        }

        @Test
        @DisplayName("Doit ignorer sans lever d'exception si l'entité ou la requête est null")
        void updateEntity_WhenNull_DoesNotThrow() {
            assembler.updateEntity(null, new BuildingRequest(303));
            assembler.updateEntity(sampleBuilding, null);

            assertThat(sampleBuilding.getNumber()).isEqualTo(101);
        }
    }

    @Nested
    @DisplayName("Tests toDtoList")
    class ToDtoListTests {

        @Test
        @DisplayName("Doit convertir une collection d'entités en liste de DTOs")
        void toDtoList_Success() {
            Building b2 = new Building();
            b2.setId(2L);
            b2.setVersion(1L);
            b2.setNumber(102);

            List<BuildingResponse> list = assembler.toDtoList(List.of(sampleBuilding, b2));

            assertThat(list).hasSize(2);
            assertThat(list.get(0).number()).isEqualTo(101);
            assertThat(list.get(1).number()).isEqualTo(102);
        }

        @Test
        @DisplayName("Doit retourner une liste vide quand la collection est null")
        void toDtoList_WhenNull_ReturnsEmptyList() {
            List<BuildingResponse> list = assembler.toDtoList(null);

            assertThat(list).isEmpty();
        }
    }
}
