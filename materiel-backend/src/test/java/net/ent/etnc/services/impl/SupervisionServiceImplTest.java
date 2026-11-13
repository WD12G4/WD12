package net.ent.etnc.services.impl;

import net.ent.etnc.dtos.supervision.KillProcessRequest;
import net.ent.etnc.dtos.supervision.KillProcessResponse;
import net.ent.etnc.dtos.supervision.ProcessInfoDto;
import net.ent.etnc.dtos.supervision.WorkstationDto;
import net.ent.etnc.models.Application;
import net.ent.etnc.models.KillEvent;
import net.ent.etnc.models.Material;
import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.EMaterialType;
import net.ent.etnc.models.enums.ERole;
import net.ent.etnc.repositories.MaterialRepository;
import net.ent.etnc.services.ApplicationService;
import net.ent.etnc.services.KillEventService;
import net.ent.etnc.services.UserService;
import net.ent.etnc.websocket.AgentWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupervisionServiceImpl Tests")
class SupervisionServiceImplTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private AgentWebSocketHandler agentWebSocketHandler;

    @Mock
    private KillEventService killEventService;

    @Mock
    private UserService userService;

    @Mock
    private ApplicationService applicationService;

    @InjectMocks
    private SupervisionServiceImpl supervisionService;

    private Material sampleUc;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUc = new Material();
        sampleUc.setId(10L);
        sampleUc.setSerialNumber("UC-TEST-01");
        sampleUc.setMaterialType(EMaterialType.UC);

        sampleUser = new User();
        sampleUser.setId(3L);
        sampleUser.setUsername("formateur");
        sampleUser.setRole(ERole.USER);
    }

    @Test
    @DisplayName("findWorkstationsByRoom retourne les postes avec statut online/offline")
    void findWorkstationsByRoom_ReturnsList() {
        when(materialRepository.findByRoomIdAndMaterialTypeAndArchivedFalse(1L, EMaterialType.UC))
                .thenReturn(List.of(sampleUc));
        when(agentWebSocketHandler.isConnected("UC-TEST-01")).thenReturn(true);
        when(agentWebSocketHandler.getConnection("UC-TEST-01")).thenReturn(Optional.of(
                new AgentWebSocketHandler.AgentConnection("UC-TEST-01", "host-01", "00:11:22:33:44:55", null, Instant.now())
        ));

        List<WorkstationDto> result = supervisionService.findWorkstationsByRoom(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(10L);
        assertThat(result.get(0).online()).isTrue();
        assertThat(result.get(0).macAddress()).isEqualTo("00:11:22:33:44:55");
    }

    @Test
    @DisplayName("findProcessesForWorkstations interroge l'agent et retourne les processus")
    void findProcessesForWorkstations_Success() {
        Application app = new Application();
        app.setName("code.exe");
        app.setBlacklisted(false);
        app.setIconBase64("data:image/png;base64,ABC");

        when(materialRepository.findById(10L)).thenReturn(Optional.of(sampleUc));
        when(agentWebSocketHandler.fetchProcesses(eq("UC-TEST-01"), anyLong())).thenReturn(List.of(
                new AgentWebSocketHandler.AgentProcessInfo(4321, "code.exe", System.currentTimeMillis(), "data:image/png;base64,ABC")
        ));
        when(applicationService.registerDiscoveredApplication(eq("code.exe"), any())).thenReturn(app);

        List<ProcessInfoDto> procs = supervisionService.findProcessesForWorkstations(List.of(10L));

        assertThat(procs).hasSize(1);
        assertThat(procs.get(0).pid()).isEqualTo(4321);
        assertThat(procs.get(0).name()).isEqualTo("code.exe");
        assertThat(procs.get(0).icon()).isEqualTo("data:image/png;base64,ABC");
    }

    @Test
    @DisplayName("findProcessesForWorkstations déclenche un auto-kill si un processus est sur liste noire")
    void findProcessesForWorkstations_WhenProcessBlacklisted_ExecutesAutoKill() {
        Application blacklistedApp = new Application();
        blacklistedApp.setName("steam.exe");
        blacklistedApp.setBlacklisted(true);

        when(materialRepository.findById(10L)).thenReturn(Optional.of(sampleUc));
        when(agentWebSocketHandler.fetchProcesses(eq("UC-TEST-01"), anyLong())).thenReturn(List.of(
                new AgentWebSocketHandler.AgentProcessInfo(9999, "steam.exe", System.currentTimeMillis())
        ));
        when(applicationService.registerDiscoveredApplication("steam.exe", null)).thenReturn(blacklistedApp);
        when(agentWebSocketHandler.executeKill("UC-TEST-01", 9999, 5))
                .thenReturn(new AgentWebSocketHandler.AgentKillResult(true, 9999, "steam.exe", null));
        when(userService.currentUser()).thenReturn(sampleUser);

        List<ProcessInfoDto> procs = supervisionService.findProcessesForWorkstations(List.of(10L));

        // Le processus doit avoir été arrêté et ne pas figurer dans la liste des processus actifs retournés
        assertThat(procs).isEmpty();
        verify(agentWebSocketHandler).executeKill("UC-TEST-01", 9999, 5);
        verify(killEventService).create(any(KillEvent.class));
    }

    @Test
    @DisplayName("killProcess réussit et enregistre un KillEvent avec result=true")
    void killProcess_Success() {
        when(materialRepository.findById(10L)).thenReturn(Optional.of(sampleUc));
        when(agentWebSocketHandler.executeKill(eq("UC-TEST-01"), eq(4321), anyLong()))
                .thenReturn(new AgentWebSocketHandler.AgentKillResult(true, 4321, "code.exe", ""));
        when(userService.currentUser()).thenReturn(sampleUser);

        KillEvent savedEvent = new KillEvent();
        savedEvent.setId(99L);
        savedEvent.setResult(true);
        when(killEventService.create(any(KillEvent.class))).thenReturn(savedEvent);

        KillProcessRequest req = new KillProcessRequest(10L, 4321, "code.exe");
        KillProcessResponse res = supervisionService.killProcess(req);

        assertThat(res.success()).isTrue();
        assertThat(res.killEventId()).isEqualTo(99L);

        ArgumentCaptor<KillEvent> captor = ArgumentCaptor.forClass(KillEvent.class);
        verify(killEventService, times(1)).create(captor.capture());
        KillEvent captured = captor.getValue();
        assertThat(captured.getResult()).isTrue();
        assertThat(captured.getProcessName()).isEqualTo("code.exe");
        assertThat(captured.getUc()).isEqualTo(sampleUc);
        assertThat(captured.getUser()).isEqualTo(sampleUser);
    }

    @Test
    @DisplayName("killProcess enregistre un KillEvent avec result=false si l'agent refuse ou échoue")
    void killProcess_WhenAgentRefuses_RecordsFailedKillEvent() {
        when(materialRepository.findById(10L)).thenReturn(Optional.of(sampleUc));
        when(agentWebSocketHandler.executeKill(eq("UC-TEST-01"), eq(1), anyLong()))
                .thenReturn(new AgentWebSocketHandler.AgentKillResult(false, 1, "systemd", "Action interdite : processus système"));
        when(userService.currentUser()).thenReturn(sampleUser);

        KillEvent failedEvent = new KillEvent();
        failedEvent.setId(101L);
        failedEvent.setResult(false);
        when(killEventService.create(any(KillEvent.class))).thenReturn(failedEvent);

        KillProcessRequest req = new KillProcessRequest(10L, 1, "systemd");
        KillProcessResponse res = supervisionService.killProcess(req);

        assertThat(res.success()).isFalse();
        assertThat(res.message()).isEqualTo("Action interdite : processus système");
        assertThat(res.killEventId()).isEqualTo(101L);

        ArgumentCaptor<KillEvent> captor = ArgumentCaptor.forClass(KillEvent.class);
        verify(killEventService, times(1)).create(captor.capture());
        KillEvent captured = captor.getValue();
        assertThat(captured.getResult()).isFalse();
        assertThat(captured.getProcessName()).isEqualTo("systemd");
        assertThat(captured.getUc()).isEqualTo(sampleUc);
        assertThat(captured.getUser()).isEqualTo(sampleUser);
    }
}
