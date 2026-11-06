package net.ent.etnc.controllers;

import net.ent.etnc.dtos.assemblers.SightingDtoAssembler;
import net.ent.etnc.dtos.sighting.SightingResponse;
import net.ent.etnc.handlers.GlobalExceptionHandler;
import net.ent.etnc.models.Sighting;
import net.ent.etnc.models.User;
import net.ent.etnc.services.SightingService;
import net.ent.etnc.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SightingController Web Tests")
class SightingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SightingService sightingService;

    @Mock
    private SightingDtoAssembler sightingAssembler;

    @Mock
    private UserService userService;

    @InjectMocks
    private SightingController sightingController;

    private SightingResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sightingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleResponse = new SightingResponse(1L, "SS4SG0548", 10L, Instant.parse("2026-11-05T01:10:00Z"), "manager");
    }

    @Test
    @DisplayName("GET : doit retourner tous les constats")
    void getAll_ReturnsSightings() throws Exception {
        when(sightingService.findAll()).thenReturn(List.of());
        when(sightingAssembler.toDtoList(any())).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/sightings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serialNumber").value("SS4SG0548"))
                .andExpect(jsonPath("$[0].roomId").value(10))
                .andExpect(jsonPath("$[0].seenBy").value("manager"));
    }

    @Nested
    @DisplayName("POST /api/v1/sightings")
    class RecordTests {

        @Test
        @DisplayName("Doit enregistrer le constat au nom de l'agent connecté")
        void record_UsesCurrentUser() throws Exception {
            User agent = new User();
            agent.setUsername("manager");
            Sighting saved = mock(Sighting.class);
            when(userService.currentUser()).thenReturn(agent);
            when(sightingService.record(eq("SS4SG0548"), eq(10L), eq(Instant.parse("2026-11-05T01:10:00Z")),
                    eq("manager"), isNull())).thenReturn(saved);
            when(sightingAssembler.toDto(saved)).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/sightings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"serialNumber": "SS4SG0548", "roomId": 10, "seenAt": "2026-11-05T01:10:00Z"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.serialNumber").value("SS4SG0548"));
        }

        @Test
        @DisplayName("Doit renvoyer 400 sans numéro de série")
        void record_WithoutSerial_Returns400() throws Exception {
            mockMvc.perform(post("/api/v1/sightings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"serialNumber": "", "roomId": 10}
                                    """))
                    .andExpect(status().isBadRequest());

            verify(sightingService, never()).record(any(), any(), any(), any(), any());
        }
    }

    @Test
    @DisplayName("DELETE : doit effacer les constats de la salle et retourner 204")
    void clearRoom_Returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/sightings").param("roomId", "10"))
                .andExpect(status().isNoContent());

        verify(sightingService).clearRoom(10L);
    }
}
