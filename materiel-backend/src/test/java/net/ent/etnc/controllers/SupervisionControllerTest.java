package net.ent.etnc.controllers;

import net.ent.etnc.dtos.supervision.KillProcessRequest;
import net.ent.etnc.dtos.supervision.KillProcessResponse;
import net.ent.etnc.dtos.supervision.ProcessInfoDto;
import net.ent.etnc.dtos.supervision.WorkstationDto;
import net.ent.etnc.handlers.GlobalExceptionHandler;
import net.ent.etnc.services.SupervisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupervisionController Tests")
class SupervisionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SupervisionService supervisionService;

    @InjectMocks
    private SupervisionController supervisionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(supervisionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("getWorkstationsByRoom doit retourner la liste des postes")
    void getWorkstationsByRoom_ReturnsList() throws Exception {
        WorkstationDto ws = new WorkstationDto(1L, 10L, "Poste 1", "Stagiaire 1", "00:11:22:33:44:55", true);
        when(supervisionService.findWorkstationsByRoom(10L)).thenReturn(List.of(ws));

        mockMvc.perform(get("/api/v1/supervision/rooms/10/workstations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Poste 1"))
                .andExpect(jsonPath("$[0].online").value(true));
    }

    @Test
    @DisplayName("getWorkstations doit retourner tous les postes")
    void getWorkstations_ReturnsAll() throws Exception {
        WorkstationDto ws = new WorkstationDto(1L, null, "bly-manager", "Poste connecté", "00:11:22:33:44:55", true);
        when(supervisionService.findWorkstations(null)).thenReturn(List.of(ws));

        mockMvc.perform(get("/api/v1/supervision/workstations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("bly-manager"))
                .andExpect(jsonPath("$[0].online").value(true));
    }

    @Test
    @DisplayName("getProcesses doit retourner les processus")
    void getProcesses_ReturnsList() throws Exception {
        ProcessInfoDto proc = new ProcessInfoDto(1234, 1L, "firefox", LocalDateTime.now());
        when(supervisionService.findProcessesForWorkstations(List.of(1L))).thenReturn(List.of(proc));

        mockMvc.perform(get("/api/v1/supervision/processes?workstationIds=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pid").value(1234))
                .andExpect(jsonPath("$[0].name").value("firefox"));
    }

    @Test
    @DisplayName("killProcess doit arrêter le processus et retourner 200")
    void killProcess_Success() throws Exception {
        KillProcessResponse response = new KillProcessResponse(true, "Processus arrêté avec succès", 100L);
        when(supervisionService.killProcess(any(KillProcessRequest.class))).thenReturn(response);

        String json = """
                {
                    "workstationId": 1,
                    "pid": 1234,
                    "processName": "firefox"
                }
                """;

        mockMvc.perform(post("/api/v1/supervision/kill")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.killEventId").value(100));
    }

    @Test
    @DisplayName("killProcess retourne 200 avec success=false et l'ID du KillEvent échoué")
    void killProcess_Failure() throws Exception {
        KillProcessResponse response = new KillProcessResponse(false, "Action refusée : processus système", 101L);
        when(supervisionService.killProcess(any(KillProcessRequest.class))).thenReturn(response);

        String json = """
                {
                    "workstationId": 1,
                    "pid": 1,
                    "processName": "systemd"
                }
                """;

        mockMvc.perform(post("/api/v1/supervision/kill")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Action refusée : processus système"))
                .andExpect(jsonPath("$.killEventId").value(101));
    }
}
