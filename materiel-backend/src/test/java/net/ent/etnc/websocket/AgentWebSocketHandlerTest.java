package net.ent.etnc.websocket;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import net.ent.etnc.models.Material;
import net.ent.etnc.models.enums.EMaterialType;
import net.ent.etnc.repositories.MaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentWebSocketHandlerTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private WebSocketSession session;

    private AgentWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AgentWebSocketHandler(materialRepository);
        lenient().when(session.getId()).thenReturn("sess-001");
        lenient().when(session.isOpen()).thenReturn(true);
    }

    @Test
    @DisplayName("afterConnectionEstablished ne lève pas d'exception et initialise la session")
    void afterConnectionEstablished_Success() {
        handler.afterConnectionEstablished(session);
        assertThat(handler.getAllConnectedAgents()).isEmpty();
    }

    @Test
    @DisplayName("handleRegister refuse l'enregistrement si le numéro de série est absent en base")
    void handleRegister_UnknownSerialNumber_ClosesSessionAndRejects() throws IOException {
        when(materialRepository.findBySerialNumber("UNKNOWN-SN")).thenReturn(Optional.empty());

        String json = """
                {"type":"REGISTER","serialNumber":"UNKNOWN-SN","hostname":"pc-test","macAddress":"AA:BB:CC:DD:EE:FF"}
                """;
        handler.handleTextMessage(session, new TextMessage(json));

        assertThat(handler.isConnected("UNKNOWN-SN")).isFalse();
        ArgumentCaptor<CloseStatus> captor = ArgumentCaptor.forClass(CloseStatus.class);
        verify(session).close(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(CloseStatus.POLICY_VIOLATION.getCode());
    }

    @Test
    @DisplayName("handleRegister refuse l'enregistrement si le numéro de série est vide")
    void handleRegister_BlankSerialNumber_ClosesSession() throws IOException {
        String json = """
                {"type":"REGISTER","serialNumber":"","hostname":"pc-test","macAddress":"AA:BB:CC:DD:EE:FF"}
                """;
        handler.handleTextMessage(session, new TextMessage(json));

        ArgumentCaptor<CloseStatus> captor = ArgumentCaptor.forClass(CloseStatus.class);
        verify(session).close(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(CloseStatus.POLICY_VIOLATION.getCode());
    }

    @Test
    @DisplayName("handleRegister refuse l'enregistrement si le matériel est archivé")
    void handleRegister_ArchivedMaterial_ClosesSessionAndRejects() throws IOException {
        Material uc = new Material();
        uc.setSerialNumber("ARCHIVED-UC");
        uc.setMaterialType(EMaterialType.UC);
        uc.setArchived(true);

        when(materialRepository.findBySerialNumber("ARCHIVED-UC")).thenReturn(Optional.of(uc));

        String json = """
                {"type":"REGISTER","serialNumber":"ARCHIVED-UC","hostname":"pc-archived","macAddress":"AA:BB:CC:DD:EE:FF"}
                """;
        handler.handleTextMessage(session, new TextMessage(json));

        assertThat(handler.isConnected("ARCHIVED-UC")).isFalse();
        verify(session).close(any(CloseStatus.class));
    }

    @Test
    @DisplayName("handleRegister refuse l'enregistrement si le matériel n'est pas de type UC")
    void handleRegister_NotUcMaterial_ClosesSessionAndRejects() throws IOException {
        Material ecran = new Material();
        ecran.setSerialNumber("ECRAN-001");
        ecran.setMaterialType(EMaterialType.SCREEN);
        ecran.setArchived(false);

        when(materialRepository.findBySerialNumber("ECRAN-001")).thenReturn(Optional.of(ecran));

        String json = """
                {"type":"REGISTER","serialNumber":"ECRAN-001","hostname":"pc-test","macAddress":"AA:BB:CC:DD:EE:FF"}
                """;
        handler.handleTextMessage(session, new TextMessage(json));

        assertThat(handler.isConnected("ECRAN-001")).isFalse();
        verify(session).close(any(CloseStatus.class));
    }

    @Test
    @DisplayName("handleRegister enregistre avec succès un agent dont l'UC existe et est active")
    void handleRegister_ValidUc_RegistersAgent() {
        Material uc = new Material();
        uc.setSerialNumber("UC-VALID-01");
        uc.setMaterialType(EMaterialType.UC);
        uc.setArchived(false);

        when(materialRepository.findBySerialNumber("UC-VALID-01")).thenReturn(Optional.of(uc));

        String json = """
                {"type":"REGISTER","serialNumber":"UC-VALID-01","hostname":"pc-lab","macAddress":"00:11:22:33:44:55","username":"stagiaire1","os":"windows"}
                """;
        handler.handleTextMessage(session, new TextMessage(json));

        assertThat(handler.isConnected("UC-VALID-01")).isTrue();
        Optional<AgentWebSocketHandler.AgentConnection> conn = handler.getConnection("UC-VALID-01");
        assertThat(conn).isPresent();
        assertThat(conn.get().hostname()).isEqualTo("pc-lab");
        assertThat(conn.get().username()).isEqualTo("stagiaire1");
        assertThat(conn.get().macAddress()).isEqualTo("00:11:22:33:44:55");
        assertThat(conn.get().os()).isEqualTo("windows");
        assertThat(handler.getAllConnectedAgents()).hasSize(1);
    }

    @Test
    @DisplayName("handleRegister remplace proprement une session précédente si une nouvelle session se connecte")
    void handleRegister_ReplacesPreviousSession() throws IOException {
        Material uc = new Material();
        uc.setSerialNumber("UC-VALID-01");
        uc.setMaterialType(EMaterialType.UC);
        uc.setArchived(false);
        when(materialRepository.findBySerialNumber("UC-VALID-01")).thenReturn(Optional.of(uc));

        WebSocketSession session2 = mock(WebSocketSession.class);
        lenient().when(session2.getId()).thenReturn("sess-002");
        lenient().when(session2.isOpen()).thenReturn(true);

        String json = """
                {"type":"REGISTER","serialNumber":"UC-VALID-01","hostname":"pc-lab","macAddress":"00:11:22:33:44:55"}
                """;
        handler.handleTextMessage(session, new TextMessage(json));

        // Nouvelle session pour la même UC
        handler.handleTextMessage(session2, new TextMessage(json));

        verify(session).close(any(CloseStatus.class));
        assertThat(handler.isConnected("UC-VALID-01")).isTrue();
    }

    @Test
    @DisplayName("handlePong et handleTextMessage inconnus sont gérés proprement")
    void handlePong_UpdatesUsername() {
        Material uc = new Material();
        uc.setSerialNumber("UC-VALID-01");
        uc.setMaterialType(EMaterialType.UC);
        uc.setArchived(false);
        when(materialRepository.findBySerialNumber("UC-VALID-01")).thenReturn(Optional.of(uc));

        String regJson = """
                {"type":"REGISTER","serialNumber":"UC-VALID-01","hostname":"pc-lab","macAddress":"00:11:22:33:44:55"}
                """;
        handler.handleTextMessage(session, new TextMessage(regJson));

        String pongJson = """
                {"type":"PONG","username":"new-user"}
                """;
        handler.handleTextMessage(session, new TextMessage(pongJson));

        assertThat(handler.getConnection("UC-VALID-01").get().username()).isEqualTo("new-user");

        // Message inconnu ou invalide
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"INCONNU\"}"));
        handler.handleTextMessage(session, new TextMessage("non-json"));
    }

    @Test
    @DisplayName("afterConnectionClosed déconnecte l'agent uniquement si la session correspondante se ferme")
    void afterConnectionClosed_RemovesAgentConnection() {
        Material uc = new Material();
        uc.setSerialNumber("UC-VALID-01");
        uc.setMaterialType(EMaterialType.UC);
        uc.setArchived(false);

        when(materialRepository.findBySerialNumber("UC-VALID-01")).thenReturn(Optional.of(uc));

        String json = """
                {"type":"REGISTER","serialNumber":"UC-VALID-01","hostname":"pc-lab","macAddress":"00:11:22:33:44:55"}
                """;
        handler.handleTextMessage(session, new TextMessage(json));
        assertThat(handler.isConnected("UC-VALID-01")).isTrue();

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        assertThat(handler.isConnected("UC-VALID-01")).isFalse();
    }

    @Test
    @DisplayName("fetchProcessesAsync renvoie une liste vide immédiate si l'agent n'est pas connecté")
    void fetchProcessesAsync_WhenNotConnected_ReturnsEmptyList() {
        CompletableFuture<List<AgentWebSocketHandler.AgentProcessInfo>> future =
                handler.fetchProcessesAsync("DISCONNECTED-UC", 3);

        assertThat(future.isDone()).isTrue();
        assertThat(future.join()).isEmpty();
    }

    @Test
    @DisplayName("fetchProcessesAsync renvoie la liste des processus reçus de l'agent")
    void fetchProcessesAsync_WhenConnected_ReturnsProcesses() throws Exception {
        Material uc = new Material();
        uc.setSerialNumber("UC-VALID-01");
        uc.setMaterialType(EMaterialType.UC);
        uc.setArchived(false);
        when(materialRepository.findBySerialNumber("UC-VALID-01")).thenReturn(Optional.of(uc));

        String regJson = """
                {"type":"REGISTER","serialNumber":"UC-VALID-01","hostname":"pc-lab","macAddress":"00:11:22:33:44:55"}
                """;
        handler.handleTextMessage(session, new TextMessage(regJson));

        doAnswer(invocation -> {
            TextMessage msg = invocation.getArgument(0);
            try (JsonReader reader = Json.createReader(new StringReader(msg.getPayload()))) {
                JsonObject root = reader.readObject();
                String reqId = root.getString("requestId");
                String listJson = """
                        {"type":"PROCESS_LIST","requestId":"%s","processes":[{"pid":100,"name":"chrome.exe","startedAt":1700000000,"icon":"data:image/png;base64,abc"}]}
                        """.formatted(reqId);
                handler.handleTextMessage(session, new TextMessage(listJson));
            }
            return null;
        }).when(session).sendMessage(any(TextMessage.class));

        CompletableFuture<List<AgentWebSocketHandler.AgentProcessInfo>> future =
                handler.fetchProcessesAsync("UC-VALID-01", 5);

        List<AgentWebSocketHandler.AgentProcessInfo> list = future.join();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).name()).isEqualTo("chrome.exe");
        assertThat(list.get(0).pid()).isEqualTo(100);
        assertThat(list.get(0).icon()).isEqualTo("data:image/png;base64,abc");
    }

    @Test
    @DisplayName("executeKill renvoie une erreur si l'agent n'est pas connecté")
    void executeKill_WhenNotConnected_ReturnsFailure() {
        AgentWebSocketHandler.AgentKillResult result = handler.executeKill("DISCONNECTED-UC", 1234, 1);
        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("Agent non connecté");
    }

    @Test
    @DisplayName("executeKill renvoie un échec sur timeout de l'agent")
    void executeKill_WhenTimeout_ReturnsTimeoutFailure() {
        Material uc = new Material();
        uc.setSerialNumber("UC-VALID-01");
        uc.setMaterialType(EMaterialType.UC);
        uc.setArchived(false);
        when(materialRepository.findBySerialNumber("UC-VALID-01")).thenReturn(Optional.of(uc));

        String json = """
                {"type":"REGISTER","serialNumber":"UC-VALID-01","hostname":"pc-lab","macAddress":"00:11:22:33:44:55"}
                """;
        handler.handleTextMessage(session, new TextMessage(json));

        AgentWebSocketHandler.AgentKillResult result = handler.executeKill("UC-VALID-01", 1234, 0);
        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("Délai d'attente dépassé");
    }

    @Test
    @DisplayName("executeKill rétablit l'état d'interruption et renvoie une erreur en cas d'InterruptedException")
    void executeKill_WhenInterrupted_RestoresInterruptedStatus() {
        Material uc = new Material();
        uc.setSerialNumber("UC-VALID-01");
        uc.setMaterialType(EMaterialType.UC);
        uc.setArchived(false);
        when(materialRepository.findBySerialNumber("UC-VALID-01")).thenReturn(Optional.of(uc));

        String json = """
                {"type":"REGISTER","serialNumber":"UC-VALID-01","hostname":"pc-lab","macAddress":"00:11:22:33:44:55"}
                """;
        handler.handleTextMessage(session, new TextMessage(json));

        Thread.currentThread().interrupt();
        AgentWebSocketHandler.AgentKillResult result = handler.executeKill("UC-VALID-01", 1234, 5);
        boolean interrupted = Thread.interrupted();
        assertThat(interrupted).isTrue();
        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("Interruption de la communication");
    }

    @Test
    @DisplayName("executeKill renvoie le résultat de l'agent si la réponse arrive à temps")
    void executeKill_WhenSuccess_ReturnsResult() throws Exception {
        Material uc = new Material();
        uc.setSerialNumber("UC-VALID-01");
        uc.setMaterialType(EMaterialType.UC);
        uc.setArchived(false);
        when(materialRepository.findBySerialNumber("UC-VALID-01")).thenReturn(Optional.of(uc));

        String json = """
                {"type":"REGISTER","serialNumber":"UC-VALID-01","hostname":"pc-lab","macAddress":"00:11:22:33:44:55"}
                """;
        handler.handleTextMessage(session, new TextMessage(json));

        doAnswer(invocation -> {
            TextMessage msg = invocation.getArgument(0);
            try (JsonReader reader = Json.createReader(new StringReader(msg.getPayload()))) {
                JsonObject root = reader.readObject();
                String reqId = root.getString("requestId");
                String responseJson = """
                        {"type":"KILL_RESULT","requestId":"%s","result":{"success":true,"pid":1234,"processName":"notepad.exe","error":""}}
                        """.formatted(reqId);
                handler.handleTextMessage(session, new TextMessage(responseJson));
            }
            return null;
        }).when(session).sendMessage(any(TextMessage.class));

        AgentWebSocketHandler.AgentKillResult result = handler.executeKill("UC-VALID-01", 1234, 5);
        assertThat(result.success()).isTrue();
        assertThat(result.pid()).isEqualTo(1234);
        assertThat(result.processName()).isEqualTo("notepad.exe");
    }
}
