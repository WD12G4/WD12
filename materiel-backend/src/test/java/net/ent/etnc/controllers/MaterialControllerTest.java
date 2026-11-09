package net.ent.etnc.controllers;

import net.ent.etnc.dtos.assemblers.MaterialDtoAssembler;
import net.ent.etnc.dtos.material.MaterialRequest;
import net.ent.etnc.dtos.material.MaterialResponse;
import net.ent.etnc.handlers.GlobalExceptionHandler;
import net.ent.etnc.models.Material;
import net.ent.etnc.models.enums.EMaterialType;
import net.ent.etnc.services.MaterialService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaterialController Web Tests")
class MaterialControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MaterialService materialService;

    @Mock
    private MaterialDtoAssembler materialAssembler;

    @InjectMocks
    private MaterialController materialController;

    private Material sampleMaterial;
    private MaterialResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(materialController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        sampleMaterial = new Material();
        sampleMaterial.setId(1L);
        sampleMaterial.setSerialNumber("SN-12345");
        sampleMaterial.setMaterialType(EMaterialType.UC);
        sampleMaterial.setEndAvailabilityDate(LocalDate.now().plusYears(1));
        sampleMaterial.setArchived(false);

        sampleResponse = new MaterialResponse(
                1L, 0L, "SN-12345", EMaterialType.UC,
                LocalDate.now().plusYears(1), null, false
        );
    }

    @Nested
    @DisplayName("GET /api/v1/materials")
    class GetAllTests {

        @Test
        @DisplayName("Doit retourner une page de matériels")
        void getAll_ReturnsPageOfMaterials() throws Exception {
            Page<Material> page = new PageImpl<>(List.of(sampleMaterial), PageRequest.of(0, 20), 1);
            when(materialService.findAll(any(Pageable.class))).thenReturn(page);
            when(materialAssembler.toDto(sampleMaterial)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/materials")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].serialNumber").value("SN-12345"))
                    .andExpect(jsonPath("$.content[0].materialType").value("UC"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/materials/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("Doit retourner le matériel quand il existe")
        void getById_WhenExists_ReturnsMaterial() throws Exception {
            when(materialService.findById(1L)).thenReturn(Optional.of(sampleMaterial));
            when(materialAssembler.toDto(sampleMaterial)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/materials/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.serialNumber").value("SN-12345"));
        }

        @Test
        @DisplayName("Doit renvoyer 404 si le matériel n'existe pas")
        void getById_WhenNotFound_Returns404() throws Exception {
            when(materialService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/materials/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/materials")
    class CreateTests {

        @Test
        @DisplayName("Doit créer le matériel et retourner 201 Created")
        void create_ValidRequest_Returns201Created() throws Exception {
            String payload = """
                    {
                        "serialNumber": "SN-12345",
                        "materialType": "UC",
                        "endAvailabilityDate": "2028-12-31",
                        "archived": false
                    }
                    """;

            when(materialAssembler.toEntity(any(MaterialRequest.class))).thenReturn(sampleMaterial);
            when(materialService.create(any(Material.class))).thenReturn(sampleMaterial);
            when(materialAssembler.toDto(sampleMaterial)).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/materials")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.serialNumber").value("SN-12345"));
        }

        @Test
        @DisplayName("Doit renvoyer 400 Bad Request si le numéro de série est vide")
        void create_BlankSerialNumber_Returns400() throws Exception {
            String payload = """
                    {
                        "serialNumber": "",
                        "materialType": "UC",
                        "endAvailabilityDate": "2028-12-31",
                        "archived": false
                    }
                    """;

            mockMvc.perform(post("/api/v1/materials")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());

            verify(materialService, never()).create(any());
        }

        @Test
        @DisplayName("Doit renvoyer 400 Bad Request si le type de matériel est manquant")
        void create_NullMaterialType_Returns400() throws Exception {
            String payload = """
                    {
                        "serialNumber": "SN-12345",
                        "endAvailabilityDate": "2028-12-31",
                        "archived": false
                    }
                    """;

            mockMvc.perform(post("/api/v1/materials")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());

            verify(materialService, never()).create(any());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/materials/{id}")
    class UpdateTests {

        @Test
        @DisplayName("Doit mettre à jour le matériel et retourner 200 OK")
        void update_ValidRequest_Returns200Ok() throws Exception {
            String payload = """
                    {
                        "serialNumber": "SN-12345",
                        "materialType": "UC",
                        "endAvailabilityDate": "2028-12-31",
                        "archived": false
                    }
                    """;

            when(materialService.findById(1L)).thenReturn(Optional.of(sampleMaterial));
            doNothing().when(materialAssembler).updateEntity(eq(sampleMaterial), any(MaterialRequest.class));
            when(materialService.update(sampleMaterial)).thenReturn(sampleMaterial);
            when(materialAssembler.toDto(sampleMaterial)).thenReturn(sampleResponse);

            mockMvc.perform(put("/api/v1/materials/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("Doit retourner 404 si le matériel à mettre à jour n'existe pas")
        void update_WhenNotFound_Returns404() throws Exception {
            String payload = """
                    {
                        "serialNumber": "SN-12345",
                        "materialType": "UC",
                        "endAvailabilityDate": "2028-12-31",
                        "archived": false
                    }
                    """;

            when(materialService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(put("/api/v1/materials/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/materials/{id}/room")
    class AssignRoomTests {

        @Test
        @DisplayName("Doit affecter le matériel à une salle et retourner 200 OK")
        void assignRoom_Success_Returns200Ok() throws Exception {
            String payload = """
                    {
                        "roomId": 5
                    }
                    """;

            when(materialService.assignRoom(1L, 5L)).thenReturn(sampleMaterial);
            when(materialAssembler.toDto(sampleMaterial)).thenReturn(sampleResponse);

            mockMvc.perform(patch("/api/v1/materials/1/room")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(materialService).assignRoom(1L, 5L);
        }

        @Test
        @DisplayName("Doit désaffecter le matériel de toute salle quand roomId est null")
        void assignRoom_NullRoomId_Returns200Ok() throws Exception {
            String payload = """
                    {
                        "roomId": null
                    }
                    """;

            when(materialService.assignRoom(1L, null)).thenReturn(sampleMaterial);
            when(materialAssembler.toDto(sampleMaterial)).thenReturn(sampleResponse);

            mockMvc.perform(patch("/api/v1/materials/1/room")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(materialService).assignRoom(1L, null);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/materials/{id}")
    class DeleteTests {

        @Test
        @DisplayName("Doit archiver le matériel et retourner 204 No Content")
        void delete_Returns204NoContent() throws Exception {
            doNothing().when(materialService).archiveById(1L);

            mockMvc.perform(delete("/api/v1/materials/1"))
                    .andExpect(status().isNoContent());

            verify(materialService).archiveById(1L);
        }
    }
}
