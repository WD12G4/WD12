package net.ent.etnc.controllers;

import net.ent.etnc.dtos.assemblers.KillEventDtoAssembler;
import net.ent.etnc.dtos.killevent.KillEventRequest;
import net.ent.etnc.dtos.killevent.KillEventResponse;
import net.ent.etnc.dtos.material.MaterialResponse;
import net.ent.etnc.handlers.GlobalExceptionHandler;
import net.ent.etnc.models.KillEvent;
import net.ent.etnc.models.Material;
import net.ent.etnc.models.enums.EMaterialType;
import net.ent.etnc.services.KillEventService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KillEventController Web Tests")
class KillEventControllerTest {

    private MockMvc mockMvc;

    @Mock
    private KillEventService killEventService;

    @Mock
    private KillEventDtoAssembler killEventDtoAssembler;

    @InjectMocks
    private KillEventController killEventController;

    private KillEvent sampleEntity;
    private KillEventResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(killEventController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        Material sampleUc = new Material();
        sampleUc.setId(1L);
        sampleUc.setSerialNumber("UC-12345");
        sampleUc.setMaterialType(EMaterialType.UC);

        sampleEntity = new KillEvent();
        sampleEntity.setId(10L);
        sampleEntity.setProcessName("malicious.exe");
        sampleEntity.setUc(sampleUc);
        sampleEntity.setKillDateTime(LocalDateTime.of(2026, 1, 15, 10, 30));
        sampleEntity.setResult(true);

        MaterialResponse ucResponse = new MaterialResponse(
                1L, 0L, "UC-12345", EMaterialType.UC, LocalDate.now().plusYears(1), null, false
        );

        sampleResponse = new KillEventResponse(
                10L, 0L, "malicious.exe", ucResponse, LocalDateTime.of(2026, 1, 15, 10, 30), true
        );
    }

    @Nested
    @DisplayName("GET /api/v1/killevents")
    class GetAllTests {

        @Test
        @DisplayName("Doit retourner une page de KillEvents")
        void getAll_ReturnsPageOfKillEvents() throws Exception {
            Page<KillEvent> page = new PageImpl<>(List.of(sampleEntity), PageRequest.of(0, 20), 1);
            when(killEventService.findAll(any(Pageable.class))).thenReturn(page);
            when(killEventDtoAssembler.toDto(sampleEntity)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/killevents")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(10))
                    .andExpect(jsonPath("$.content[0].processName").value("malicious.exe"))
                    .andExpect(jsonPath("$.content[0].uc.serialNumber").value("UC-12345"))
                    .andExpect(jsonPath("$.content[0].result").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/killevents/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("Doit retourner le KillEvent quand il existe")
        void getById_WhenExists_ReturnsKillEvent() throws Exception {
            when(killEventService.findById(10L)).thenReturn(Optional.of(sampleEntity));
            when(killEventDtoAssembler.toDto(sampleEntity)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/killevents/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.processName").value("malicious.exe"))
                    .andExpect(jsonPath("$.uc.serialNumber").value("UC-12345"))
                    .andExpect(jsonPath("$.result").value(true));
        }

        @Test
        @DisplayName("Doit renvoyer 404 si le KillEvent n'existe pas")
        void getById_WhenNotFound_Returns404() throws Exception {
            when(killEventService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/killevents/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/killevents")
    class CreateTests {

        @Test
        @DisplayName("Doit créer le KillEvent et retourner 201 Created")
        void create_ValidRequest_Returns201Created() throws Exception {
            String payload = """
                    {
                        "processName": "malicious.exe",
                        "ucId": 1,
                        "killDateTime": "2026-01-15T10:30:00",
                        "result": true
                    }
                    """;

            when(killEventDtoAssembler.toEntity(any(KillEventRequest.class))).thenReturn(sampleEntity);
            when(killEventService.create(any(KillEvent.class))).thenReturn(sampleEntity);
            when(killEventDtoAssembler.toDto(sampleEntity)).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/killevents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.processName").value("malicious.exe"))
                    .andExpect(jsonPath("$.result").value(true));
        }

        @Test
        @DisplayName("Doit renvoyer 400 Bad Request si le nom du processus est vide")
        void create_BlankProcessName_Returns400() throws Exception {
            String payload = """
                    {
                        "processName": "",
                        "ucId": 1,
                        "killDateTime": "2026-01-15T10:30:00",
                        "result": true
                    }
                    """;

            mockMvc.perform(post("/api/v1/killevents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Doit renvoyer 400 Bad Request si l'ucId est manquant")
        void create_NullUcId_Returns400() throws Exception {
            String payload = """
                    {
                        "processName": "malicious.exe",
                        "ucId": null,
                        "killDateTime": "2026-01-15T10:30:00",
                        "result": true
                    }
                    """;

            mockMvc.perform(post("/api/v1/killevents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Doit renvoyer 400 Bad Request si la date est dans le futur")
        void create_FutureKillDateTime_Returns400() throws Exception {
            String payload = """
                    {
                        "processName": "malicious.exe",
                        "ucId": 1,
                        "killDateTime": "2099-12-31T23:59:59",
                        "result": true
                    }
                    """;

            mockMvc.perform(post("/api/v1/killevents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Doit propager 404 si l'UC est introuvable côté service")
        void create_WhenUcNotFound_Returns404() throws Exception {
            String payload = """
                    {
                        "processName": "malicious.exe",
                        "ucId": 999,
                        "killDateTime": "2026-01-15T10:30:00",
                        "result": true
                    }
                    """;

            when(killEventDtoAssembler.toEntity(any(KillEventRequest.class))).thenReturn(sampleEntity);
            when(killEventService.create(any(KillEvent.class)))
                    .thenThrow(new ResourceNotFoundException("UC non trouvée avec l'id : 999"));

            mockMvc.perform(post("/api/v1/killevents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("UC non trouvée avec l'id : 999"));
        }

        @Test
        @DisplayName("Doit propager 400 si le service rejette la règle métier (ex: pas une UC)")
        void create_WhenServiceException_Returns400() throws Exception {
            String payload = """
                    {
                        "processName": "malicious.exe",
                        "ucId": 1,
                        "killDateTime": "2026-01-15T10:30:00",
                        "result": true
                    }
                    """;

            when(killEventDtoAssembler.toEntity(any(KillEventRequest.class))).thenReturn(sampleEntity);
            when(killEventService.create(any(KillEvent.class)))
                    .thenThrow(new ServiceException("Le matériel doit être une UC pour avoir un kill event."));

            mockMvc.perform(post("/api/v1/killevents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Le matériel doit être une UC pour avoir un kill event."));
        }
    }
}
