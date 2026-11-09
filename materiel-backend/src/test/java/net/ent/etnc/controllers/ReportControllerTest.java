package net.ent.etnc.controllers;

import net.ent.etnc.dtos.assemblers.ReportDtoAssembler;
import net.ent.etnc.dtos.auth.UserSummaryResponse;
import net.ent.etnc.dtos.material.MaterialResponse;
import net.ent.etnc.dtos.report.ReportCreateRequest;
import net.ent.etnc.dtos.report.ReportResponse;
import net.ent.etnc.dtos.report.ReportUpdateRequest;
import net.ent.etnc.handlers.GlobalExceptionHandler;
import net.ent.etnc.models.Material;
import net.ent.etnc.models.Report;
import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.EMaterialType;
import net.ent.etnc.models.enums.ERole;
import net.ent.etnc.services.ReportService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import net.ent.etnc.services.commons.ServiceException;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportController Web Tests")
class ReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReportService reportService;

    @Mock
    private ReportDtoAssembler reportDtoAssembler;

    @InjectMocks
    private ReportController reportController;

    private Report sampleEntity;
    private ReportResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reportController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "john_doe", "password", List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        User sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("john_doe");
        sampleUser.setRole(ERole.USER);

        Material sampleMaterial = new Material();
        sampleMaterial.setId(10L);
        sampleMaterial.setSerialNumber("UC-001");
        sampleMaterial.setMaterialType(EMaterialType.UC);

        sampleEntity = new Report();
        sampleEntity.setId(100L);
        sampleEntity.setDescription("Écran défectueux");
        sampleEntity.setUser(sampleUser);
        sampleEntity.setMaterial(sampleMaterial);
        sampleEntity.setReportDateTime(LocalDateTime.of(2026, 2, 10, 14, 0));
        sampleEntity.setHandled(false);

        UserSummaryResponse userSummary = new UserSummaryResponse(1L, "john_doe", ERole.USER);
        MaterialResponse materialResponse = new MaterialResponse(
                10L, 0L, "UC-001", EMaterialType.UC, LocalDate.now().plusYears(1), null, false
        );

        sampleResponse = new ReportResponse(
                100L, 0L, "Écran défectueux",
                LocalDateTime.of(2026, 2, 10, 14, 0),
                userSummary, materialResponse, null, false
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /api/v1/reports")
    class GetAllTests {

        @Test
        @DisplayName("Doit retourner une page de signalements")
        void getAll_ReturnsPageOfReports() throws Exception {
            Page<Report> page = new PageImpl<>(List.of(sampleEntity), PageRequest.of(0, 20), 1);
            when(reportService.findAll(any(Pageable.class))).thenReturn(page);
            when(reportDtoAssembler.toDto(sampleEntity)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/reports")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(100))
                    .andExpect(jsonPath("$.content[0].description").value("Écran défectueux"))
                    .andExpect(jsonPath("$.content[0].user.username").value("john_doe"));
        }

        @Test
        @DisplayName("Doit filtrer par matériel quand materialId est fourni")
        void getAll_WithMaterialId_CallsFindByMaterialId() throws Exception {
            Page<Report> page = new PageImpl<>(List.of(sampleEntity), PageRequest.of(0, 20), 1);
            when(reportService.findByMaterialId(eq(10L), any(Pageable.class))).thenReturn(page);
            when(reportDtoAssembler.toDto(sampleEntity)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/reports")
                            .param("materialId", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(100));

            verify(reportService).findByMaterialId(eq(10L), any(Pageable.class));
        }

        @Test
        @DisplayName("Doit filtrer par utilisateur quand userId est fourni")
        void getAll_WithUserId_CallsFindByUserId() throws Exception {
            Page<Report> page = new PageImpl<>(List.of(sampleEntity), PageRequest.of(0, 20), 1);
            when(reportService.findByUserId(eq(1L), any(Pageable.class))).thenReturn(page);
            when(reportDtoAssembler.toDto(sampleEntity)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/reports")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(100));

            verify(reportService).findByUserId(eq(1L), any(Pageable.class));
        }

        @Test
        @DisplayName("Doit filtrer par statut de traitement quand handled est fourni")
        void getAll_WithHandled_CallsFindByHandled() throws Exception {
            Page<Report> page = new PageImpl<>(List.of(sampleEntity), PageRequest.of(0, 20), 1);
            when(reportService.findByHandled(eq(false), any(Pageable.class))).thenReturn(page);
            when(reportDtoAssembler.toDto(sampleEntity)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/reports")
                            .param("handled", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(100));

            verify(reportService).findByHandled(eq(false), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reports/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("Doit retourner le signalement quand il existe")
        void getById_WhenExists_ReturnsReport() throws Exception {
            when(reportService.findById(100L)).thenReturn(Optional.of(sampleEntity));
            when(reportDtoAssembler.toDto(sampleEntity)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/reports/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100))
                    .andExpect(jsonPath("$.description").value("Écran défectueux"));
        }

        @Test
        @DisplayName("Doit renvoyer 404 si le signalement n'existe pas")
        void getById_WhenNotFound_Returns404() throws Exception {
            when(reportService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/reports/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/reports")
    class CreateTests {

        @Test
        @DisplayName("Doit créer le signalement et retourner 201 Created")
        void create_ValidRequest_Returns201Created() throws Exception {
            String payload = """
                    {
                        "description": "Problème d'affichage persistant",
                        "materialId": 10
                    }
                    """;

            when(reportDtoAssembler.toEntity(any(ReportCreateRequest.class))).thenReturn(sampleEntity);
            when(reportService.create(any(Report.class))).thenReturn(sampleEntity);
            when(reportDtoAssembler.toDto(sampleEntity)).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(100))
                    .andExpect(jsonPath("$.description").value("Écran défectueux"));
        }

        @Test
        @DisplayName("Doit renvoyer 400 si la description est vide")
        void create_BlankDescription_Returns400() throws Exception {
            String payload = """
                    {
                        "description": "",
                        "materialId": 10
                    }
                    """;

            mockMvc.perform(post("/api/v1/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Doit renvoyer 400 si le materialId est manquant")
        void create_NullMaterialId_Returns400() throws Exception {
            String payload = """
                    {
                        "description": "Description valide",
                        "materialId": null
                    }
                    """;

            mockMvc.perform(post("/api/v1/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Doit renvoyer 400 si la description dépasse 500 caractères")
        void create_TooLongDescription_Returns400() throws Exception {
            String payload = String.format("""
                    {
                        "description": "%s",
                        "materialId": 10
                    }
                    """, "x".repeat(501));

            mockMvc.perform(post("/api/v1/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/reports/{id}")
    class UpdateTests {

        @Test
        @DisplayName("Doit modifier la description et retourner 200 OK")
        void update_ValidRequest_Returns200() throws Exception {
            String payload = """
                    {
                        "description": "Nouvelle description"
                    }
                    """;

            when(reportService.findById(100L)).thenReturn(Optional.of(sampleEntity));
            when(reportService.update(any(Report.class))).thenReturn(sampleEntity);
            when(reportDtoAssembler.toDto(sampleEntity)).thenReturn(sampleResponse);

            mockMvc.perform(put("/api/v1/reports/100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100));

            verify(reportDtoAssembler).updateEntity(eq(sampleEntity), any(ReportUpdateRequest.class));
            verify(reportService).update(sampleEntity);
        }

        @Test
        @DisplayName("Doit renvoyer 404 si le signalement à modifier n'existe pas")
        void update_WhenNotFound_Returns404() throws Exception {
            String payload = """
                    {
                        "description": "Nouvelle description"
                    }
                    """;

            when(reportService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(put("/api/v1/reports/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Doit renvoyer 403 Forbidden si l'utilisateur n'est pas le créateur")
        void update_WhenNotCreator_Returns403() throws Exception {
            String payload = """
                    {
                        "description": "Nouvelle description"
                    }
                    """;

            when(reportService.findById(100L)).thenReturn(Optional.of(sampleEntity));
            when(reportService.update(any(Report.class)))
                    .thenThrow(new AccessDeniedException("Seul l'utilisateur ayant créé le signalement peut le modifier."));

            mockMvc.perform(put("/api/v1/reports/100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Doit renvoyer 400 Bad Request si le signalement est déjà traité")
        void update_WhenAlreadyHandled_Returns400() throws Exception {
            String payload = """
                    {
                        "description": "Nouvelle description"
                    }
                    """;

            when(reportService.findById(100L)).thenReturn(Optional.of(sampleEntity));
            when(reportService.update(any(Report.class)))
                    .thenThrow(new ServiceException("Impossible de modifier un signalement déjà traité."));

            mockMvc.perform(put("/api/v1/reports/100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Impossible de modifier un signalement déjà traité."));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/reports/{id}/handle")
    class HandleTests {

        @Test
        @DisplayName("Doit mettre à jour le statut handled et retourner 200 OK")
        void markAsHandled_Success_Returns200() throws Exception {
            ReportResponse handledResponse = new ReportResponse(
                    100L, 1L, "Écran défectueux",
                    LocalDateTime.of(2026, 2, 10, 14, 0),
                    sampleResponse.user(), sampleResponse.material(),
                    LocalDateTime.of(2026, 2, 11, 9, 0), true
            );

            when(reportService.markAsHandled(100L, true)).thenReturn(sampleEntity);
            when(reportDtoAssembler.toDto(sampleEntity)).thenReturn(handledResponse);

            mockMvc.perform(patch("/api/v1/reports/100/handle")
                            .param("handled", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.handled").value(true))
                    .andExpect(jsonPath("$.handleDateTime").exists());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/reports/{id}")
    class DeleteTests {

        @Test
        @DisplayName("Doit supprimer et retourner 204 No Content")
        void delete_Success_Returns204() throws Exception {
            doNothing().when(reportService).deleteById(100L);

            mockMvc.perform(delete("/api/v1/reports/100"))
                    .andExpect(status().isNoContent());

            verify(reportService).deleteById(100L);
        }

        @Test
        @DisplayName("Doit renvoyer 403 si l'utilisateur n'est ni le créateur ni manager")
        void delete_WhenUnauthorized_Returns403() throws Exception {
            doThrow(new AccessDeniedException("Vous n'êtes pas autorisé à supprimer ce signalement."))
                    .when(reportService).deleteById(100L);

            mockMvc.perform(delete("/api/v1/reports/100"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").exists());
        }
    }
}
