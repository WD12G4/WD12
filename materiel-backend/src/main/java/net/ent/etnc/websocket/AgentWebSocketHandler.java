package net.ent.etnc.websocket;

import jakarta.json.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.StringReader;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Gestionnaire WebSocket pour les agents de supervision installés sur les PC.
 */
@Slf4j
@Component
public class AgentWebSocketHandler extends TextWebSocketHandler {

    /**
     * Sessions des agents connectés, indexées par numéro de série de l'UC.
     */
    private final Map<String, AgentConnection> connectedAgents = new ConcurrentHashMap<>();

    /**
     * Index inversé : ID de session WebSocket -> Numéro de série de l'UC.
     */
    private final Map<String, String> sessionToSerialNumber = new ConcurrentHashMap<>();

    /**
     * Requêtes en attente de réponse (requestId -> CompletableFuture).
     */
    private final Map<String, CompletableFuture<List<AgentProcessInfo>>> pendingProcessRequests = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<AgentKillResult>> pendingKillRequests = new ConcurrentHashMap<>();

    /**
     * Agent connecté. os : système remonté par l'agent (« windows », « linux », « osx »), ou null.
     */
    public record AgentConnection(
            String serialNumber,
            String hostname,
            String macAddress,
            String username,
            String os,
            WebSocketSession session,
            Instant connectedAt
    ) {
        public AgentConnection(String serialNumber, String hostname, String macAddress, WebSocketSession session, Instant connectedAt) {
            this(serialNumber, hostname, macAddress, null, null, session, connectedAt);
        }

        public AgentConnection(String serialNumber, String hostname, String macAddress, String username, WebSocketSession session, Instant connectedAt) {
            this(serialNumber, hostname, macAddress, username, null, session, connectedAt);
        }

        public AgentConnection withUsername(String newUsername) {
            return new AgentConnection(serialNumber, hostname, macAddress, newUsername, os, session, connectedAt);
        }
    }

    public record AgentProcessInfo(
            int pid,
            String name,
            long startedAt,
            String icon
    ) {
        public AgentProcessInfo(int pid, String name, long startedAt) {
            this(pid, name, startedAt, null);
        }
    }

    public record AgentKillResult(
            boolean success,
            int pid,
            String processName,
            String error
    ) {}

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Nouvelle connexion WebSocket agent établie [sessionId={}]", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try (JsonReader reader = Json.createReader(new StringReader(message.getPayload()))) {
            JsonObject root = reader.readObject();
            String type = root.getString("type", "");

            switch (type) {
                case "REGISTER" -> handleRegister(session, root);
                case "PROCESS_LIST" -> handleProcessList(session, root);
                case "KILL_RESULT" -> handleKillResult(root);
                case "PONG" -> handlePong(session, root);
                default -> log.warn("Message WebSocket agent inconnu : {}", type);
            }
        } catch (Exception e) {
            log.error("Erreur de décodage du message WebSocket agent : {}", e.getMessage());
        }
    }

    private void handlePong(WebSocketSession session, JsonObject root) {
        log.trace("Pong reçu de l'agent [sessionId={}]", session.getId());
        updateUsernameIfPresent(session, root);
    }

    private void handleRegister(WebSocketSession session, JsonObject root) {
        String serialNumber = root.getString("serialNumber", "");
        String hostname = root.getString("hostname", "");
        String macAddress = root.getString("macAddress", "");
        String username = root.containsKey("username") && !root.isNull("username")
                ? root.getString("username", null)
                : null;
        String os = root.containsKey("os") && !root.isNull("os")
                ? root.getString("os", null)
                : null;

        if (serialNumber.isBlank()) {
            log.warn("Tentative d'enregistrement agent sans serialNumber [sessionId={}]", session.getId());
            return;
        }

        AgentConnection connection = new AgentConnection(serialNumber, hostname, macAddress, username, os, session, Instant.now());
        connectedAgents.put(serialNumber, connection);
        sessionToSerialNumber.put(session.getId(), serialNumber);

        log.info("Agent enregistré avec succès [serialNumber={}, hostname={}, macAddress={}, username={}]",
                serialNumber, hostname, macAddress, username);
    }

    private void handleProcessList(WebSocketSession session, JsonObject root) {
        updateUsernameIfPresent(session, root);

        String requestId = root.getString("requestId", "");
        CompletableFuture<List<AgentProcessInfo>> future = pendingProcessRequests.remove(requestId);
        if (future == null) {
            log.debug("Réception d'une liste de processus sans requête en attente [requestId={}]", requestId);
            return;
        }

        List<AgentProcessInfo> processes = new ArrayList<>();
        if (root.containsKey("processes") && !root.isNull("processes")) {
            JsonArray list = root.getJsonArray("processes");
            for (JsonValue itemValue : list) {
                if (itemValue.getValueType() == JsonValue.ValueType.OBJECT) {
                    JsonObject item = itemValue.asJsonObject();
                    int pid = item.getInt("pid", 0);
                    String name = item.getString("name", "");
                    long startedAt = item.containsKey("startedAt") ? item.getJsonNumber("startedAt").longValue() : System.currentTimeMillis();
                    String icon = item.containsKey("icon") && !item.isNull("icon") ? item.getString("icon") : null;
                    processes.add(new AgentProcessInfo(pid, name, startedAt, icon));
                }
            }
        }
        future.complete(processes);
    }

    private void updateUsernameIfPresent(WebSocketSession session, JsonObject root) {
        if (root.containsKey("username") && !root.isNull("username")) {
            String updatedUser = root.getString("username", "");
            if (!updatedUser.isBlank()) {
                String serialNumber = sessionToSerialNumber.get(session.getId());
                if (serialNumber != null) {
                    connectedAgents.computeIfPresent(serialNumber, (s, conn) -> conn.withUsername(updatedUser));
                }
            }
        }
    }

    private void handleKillResult(JsonObject root) {
        String requestId = root.getString("requestId", "");
        CompletableFuture<AgentKillResult> future = pendingKillRequests.remove(requestId);
        if (future == null) {
            log.debug("Réception d'un résultat de kill sans requête en attente [requestId={}]", requestId);
            return;
        }

        JsonObject resNode = root.getJsonObject("result");
        if (resNode != null) {
            boolean success = resNode.getBoolean("success", false);
            int pid = resNode.getInt("pid", 0);
            String processName = resNode.getString("processName", "");
            String error = resNode.getString("error", "");
            future.complete(new AgentKillResult(success, pid, processName, error));
        } else {
            future.complete(new AgentKillResult(false, 0, "", "Réponse invalide de l'agent"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String serialNumber = sessionToSerialNumber.remove(session.getId());
        if (serialNumber != null) {
            connectedAgents.remove(serialNumber);
            log.info("Agent déconnecté [serialNumber={}, status={}]", serialNumber, status);
        }
    }

    public boolean isConnected(String serialNumber) {
        AgentConnection conn = connectedAgents.get(serialNumber);
        return conn != null && conn.session().isOpen();
    }

    public Optional<AgentConnection> getConnection(String serialNumber) {
        return Optional.ofNullable(connectedAgents.get(serialNumber));
    }

    public Collection<AgentConnection> getAllConnectedAgents() {
        return Collections.unmodifiableCollection(connectedAgents.values());
    }

    public List<AgentProcessInfo> fetchProcesses(String serialNumber, long timeoutSeconds) {
        AgentConnection conn = connectedAgents.get(serialNumber);
        if (conn == null || !conn.session().isOpen()) {
            log.debug("Impossible de récupérer les processus : agent non connecté pour UC={}", serialNumber);
            return Collections.emptyList();
        }

        String requestId = UUID.randomUUID().toString();
        CompletableFuture<List<AgentProcessInfo>> future = new CompletableFuture<>();
        pendingProcessRequests.put(requestId, future);

        try {
            JsonObject cmd = Json.createObjectBuilder()
                    .add("action", "GET_PROCESSES")
                    .add("requestId", requestId)
                    .build();
            conn.session().sendMessage(new TextMessage(cmd.toString()));
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            pendingProcessRequests.remove(requestId);
            log.warn("Timeout lors de la récupération des processus pour UC={}", serialNumber);
            return Collections.emptyList();
        } catch (Exception e) {
            pendingProcessRequests.remove(requestId);
            log.error("Erreur lors de la demande de processus pour UC={} : {}", serialNumber, e.getMessage());
            return Collections.emptyList();
        }
    }

    public AgentKillResult executeKill(String serialNumber, int pid, long timeoutSeconds) {
        AgentConnection conn = connectedAgents.get(serialNumber);
        if (conn == null || !conn.session().isOpen()) {
            return new AgentKillResult(false, pid, "", "Agent non connecté sur cette UC");
        }

        String requestId = UUID.randomUUID().toString();
        CompletableFuture<AgentKillResult> future = new CompletableFuture<>();
        pendingKillRequests.put(requestId, future);

        try {
            JsonObject cmd = Json.createObjectBuilder()
                    .add("action", "KILL_PROCESS")
                    .add("requestId", requestId)
                    .add("pid", pid)
                    .build();
            conn.session().sendMessage(new TextMessage(cmd.toString()));
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            pendingKillRequests.remove(requestId);
            return new AgentKillResult(false, pid, "", "Délai d'attente dépassé pour la réponse de l'agent");
        } catch (Exception e) {
            pendingKillRequests.remove(requestId);
            return new AgentKillResult(false, pid, "", "Erreur de communication : " + e.getMessage());
        }
    }
}
