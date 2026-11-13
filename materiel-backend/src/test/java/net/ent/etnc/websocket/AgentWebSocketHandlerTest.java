package net.ent.etnc.websocket;

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
                {"type":"REGISTER","serialNumber":"UC-VALID-01","hostname":"pc-lab","macAddress":"00:11:22:33:44:55","username":"stagiaire1"}
                """;
        handler.handleTextMessage(session, new TextMessage(json));

        assertThat(handler.isConnected("UC-VALID-01")).isTrue();
        Optional<AgentWebSocketHandler.AgentConnection> conn = handler.getConnection("UC-VALID-01");
        assertThat(conn).isPresent();
        assertThat(conn.get().hostname()).isEqualTo("pc-lab");
        assertThat(conn.get().username()).isEqualTo("stagiaire1");
        assertThat(conn.get().macAddress()).isEqualTo("00:11:22:33:44:55");
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
}
