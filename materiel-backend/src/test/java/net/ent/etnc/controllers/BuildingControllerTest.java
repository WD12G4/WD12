package net.ent.etnc.controllers;

import net.ent.etnc.dtos.assemblers.BuildingDtoAssembler;
import net.ent.etnc.dtos.building.BuildingRequest;
import net.ent.etnc.dtos.building.BuildingResponse;
import net.ent.etnc.handlers.GlobalExceptionHandler;
import net.ent.etnc.models.Building;
import net.ent.etnc.services.BuildingService;
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
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BuildingController Web Tests")
class BuildingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BuildingService buildingService;

    @Mock
    private BuildingDtoAssembler buildingAssembler;

    @InjectMocks
    private BuildingController buildingController;

    private Building sampleBuilding;
    private BuildingResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(buildingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        sampleBuilding = new Building();
        sampleBuilding.setId(1L);
        sampleBuilding.setNumber(101);

        sampleResponse = new BuildingResponse(1L, 0L, 101);
    }

    @Nested
    @DisplayName("GET /api/v1/buildings")
    class GetAllTests {

        @Test
        @DisplayName("Doit retourner une page de bâtiments")
        void getAll_ReturnsPageOfBuildings() throws Exception {
            Page<Building> page = new PageImpl<>(List.of(sampleBuilding), PageRequest.of(0, 20), 1);
            when(buildingService.findAll(any(Pageable.class))).thenReturn(page);
            when(buildingAssembler.toDto(sampleBuilding)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/buildings")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].number").value(101));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/buildings/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("Doit retourner le bâtiment quand il existe")
        void getById_WhenExists_ReturnsBuilding() throws Exception {
            when(buildingService.findById(1L)).thenReturn(Optional.of(sampleBuilding));
            when(buildingAssembler.toDto(sampleBuilding)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/buildings/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.number").value(101));
        }

        @Test
        @DisplayName("Doit renvoyer 404 si le bâtiment n'existe pas")
        void getById_WhenNotFound_Returns404() throws Exception {
            when(buildingService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/buildings/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/buildings")
    class CreateTests {

        @Test
        @DisplayName("Doit créer le bâtiment et retourner 201 Created")
        void create_ValidRequest_Returns201Created() throws Exception {
            String payload = """
                    {
                        "number": 101
                    }
                    """;

            when(buildingAssembler.toEntity(any(BuildingRequest.class))).thenReturn(sampleBuilding);
            when(buildingService.create(any(Building.class))).thenReturn(sampleBuilding);
            when(buildingAssembler.toDto(sampleBuilding)).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/buildings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.number").value(101));
        }

        @Test
        @DisplayName("Doit renvoyer 400 Bad Request si le numéro est null")
        void create_NullNumber_Returns400() throws Exception {
            String payload = "{}";

            mockMvc.perform(post("/api/v1/buildings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());

            verify(buildingService, never()).create(any());
        }

        @Test
        @DisplayName("Doit renvoyer 400 Bad Request si le numéro est négatif")
        void create_NegativeNumber_Returns400() throws Exception {
            String payload = """
                    {
                        "number": -5
                    }
                    """;

            mockMvc.perform(post("/api/v1/buildings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());

            verify(buildingService, never()).create(any());
        }

        @Test
        @DisplayName("Doit renvoyer 400 Bad Request si le numéro dépasse 999")
        void create_NumberGreaterThan999_Returns400() throws Exception {
            String payload = """
                    {
                        "number": 1000
                    }
                    """;

            mockMvc.perform(post("/api/v1/buildings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());

            verify(buildingService, never()).create(any());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/buildings/{id}")
    class UpdateTests {

        @Test
        @DisplayName("Doit mettre à jour le bâtiment et retourner 200 OK")
        void update_ValidRequest_Returns200Ok() throws Exception {
            String payload = """
                    {
                        "number": 102
                    }
                    """;

            when(buildingService.findById(1L)).thenReturn(Optional.of(sampleBuilding));
            doNothing().when(buildingAssembler).updateEntity(eq(sampleBuilding), any(BuildingRequest.class));
            when(buildingService.update(sampleBuilding)).thenReturn(sampleBuilding);
            when(buildingAssembler.toDto(sampleBuilding)).thenReturn(sampleResponse);

            mockMvc.perform(put("/api/v1/buildings/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("Doit retourner 404 si le bâtiment à mettre à jour n'existe pas")
        void update_WhenNotFound_Returns404() throws Exception {
            String payload = """
                    {
                        "number": 102
                    }
                    """;

            when(buildingService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(put("/api/v1/buildings/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/buildings/{id}")
    class DeleteTests {

        @Test
        @DisplayName("Doit supprimer le bâtiment et retourner 204 No Content")
        void delete_WhenExists_Returns204NoContent() throws Exception {
            when(buildingService.existsById(1L)).thenReturn(true);
            doNothing().when(buildingService).deleteById(1L);

            mockMvc.perform(delete("/api/v1/buildings/1"))
                    .andExpect(status().isNoContent());

            verify(buildingService).deleteById(1L);
        }

        @Test
        @DisplayName("Doit renvoyer 404 si le bâtiment à supprimer n'existe pas")
        void delete_WhenNotFound_Returns404() throws Exception {
            when(buildingService.existsById(999L)).thenReturn(false);

            mockMvc.perform(delete("/api/v1/buildings/999"))
                    .andExpect(status().isNotFound());

            verify(buildingService, never()).deleteById(anyLong());
        }
    }
}
