package net.ent.etnc.websocket;

import jakarta.json.*;
import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.models.Material;
import net.ent.etnc.models.enums.EMaterialType;
import net.ent.etnc.repositories.MaterialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
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

    private final MaterialRepository materialRepository;

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

    @Autowired
    public AgentWebSocketHandler(final MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

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

        public AgentConnection withUsername(final String newUsername) {
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
    public void afterConnectionEstablished(final WebSocketSession session) {
        log.info("Nouvelle connexion WebSocket agent établie [sessionId={}]", session.getId());
    }

    @Override
    protected void handleTextMessage(final WebSocketSession session, final TextMessage message) {
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

    private void handlePong(final WebSocketSession session, final JsonObject root) {
        log.trace("Pong reçu de l'agent [sessionId={}]", session.getId());
        updateUsernameIfPresent(session, root);
    }

    private void handleRegister(final WebSocketSession session, final JsonObject root) {
        String serialNumber = root.getString("serialNumber", "");
        String hostname = root.getString("hostname", "");
        String macAddress = root.getString("macAddress", "");
        String username = root.containsKey("username") && !root.isNull("username") ? root.getString("username", null) : null;
        String os = root.containsKey("os") && !root.isNull("os") ? root.getString("os", null) : null;

        if (serialNumber.isBlank()) {
            log.warn("Tentative d'enregistrement agent sans serialNumber [sessionId={}]", session.getId());
            closeSessionQuietly(session, CloseStatus.POLICY_VIOLATION.withReason("serialNumber obligatoire"));
            return;
        }

        // Vérification en base de données : l'UC doit exister, être non archivée et de type UC (Point C3)
        Optional<Material> ucOpt = materialRepository.findBySerialNumber(serialNumber);
        if (ucOpt.isEmpty() || ucOpt.get().isArchived()) {
            log.warn("Enregistrement agent refusé : matériel non déclaré en base ou archivé [serialNumber={}, sessionId={}]",
                    serialNumber, session.getId());
            closeSessionQuietly(session, CloseStatus.POLICY_VIOLATION.withReason("Matériel inconnu ou archivé"));
            return;
        }

        Material uc = ucOpt.get();
        if (!EMaterialType.UC.equals(uc.getMaterialType())) {
            log.warn("Enregistrement agent refusé : le matériel n'est pas une UC [serialNumber={}, type={}, sessionId={}]",
                    serialNumber, uc.getMaterialType(), session.getId());
            closeSessionQuietly(session, CloseStatus.POLICY_VIOLATION.withReason("Le matériel n'est pas une unité centrale (UC)"));
            return;
        }

        // Envelopper la session dans ConcurrentWebSocketSessionDecorator pour sécuriser les écritures concurrentes (Point I5)
        WebSocketSession threadSafeSession = session instanceof ConcurrentWebSocketSessionDecorator
                ? session
                : new ConcurrentWebSocketSessionDecorator(session, 10000, 512 * 1024);

        AgentConnection connection = new AgentConnection(serialNumber, hostname, macAddress, username, os, threadSafeSession, Instant.now());
        AgentConnection previous = connectedAgents.put(serialNumber, connection);
        sessionToSerialNumber.put(session.getId(), serialNumber);

        // Si une ancienne session était active pour ce numéro de série, on la ferme proprement
        if (previous != null && previous.session().isOpen() && !previous.session().getId().equals(session.getId())) {
            log.info("Remplacement de session pour serialNumber={} [ancienneSession={}, nouvelleSession={}]",
                    serialNumber, previous.session().getId(), session.getId());
            closeSessionQuietly(previous.session(), CloseStatus.NORMAL.withReason("Nouvelle session connectée"));
        }

        log.info("Agent enregistré avec succès [serialNumber={}, hostname={}, macAddress={}, username={}]",
                serialNumber, hostname, macAddress, username);
    }

    private void handleProcessList(final WebSocketSession session, final JsonObject root) {
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

    private void updateUsernameIfPresent(final WebSocketSession session, final JsonObject root) {
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

    private void handleKillResult(final JsonObject root) {
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
    public void afterConnectionClosed(final WebSocketSession session, final CloseStatus status) {
        String serialNumber = sessionToSerialNumber.remove(session.getId());
        if (serialNumber != null) {
            // Nettoyage ciblé par session : évite de supprimer une reconnexion survenue entre-temps (Point I4)
            connectedAgents.computeIfPresent(serialNumber, (s, conn) ->
                    conn.session().getId().equals(session.getId()) ? null : conn
            );
            log.info("Agent déconnecté [serialNumber={}, sessionId={}, status={}]", serialNumber, session.getId(), status);
        }
    }

    public boolean isConnected(final String serialNumber) {
        AgentConnection conn = connectedAgents.get(serialNumber);
        return conn != null && conn.session().isOpen();
    }

    public Optional<AgentConnection> getConnection(final String serialNumber) {
        return Optional.ofNullable(connectedAgents.get(serialNumber));
    }

    public Collection<AgentConnection> getAllConnectedAgents() {
        return Collections.unmodifiableCollection(connectedAgents.values());
    }

    /**
     * Interroge l'agent de façon asynchrone pour récupérer la liste de ses processus (Point I5).
     */
    public CompletableFuture<List<AgentProcessInfo>> fetchProcessesAsync(final String serialNumber, final long timeoutSeconds) {
        AgentConnection conn = connectedAgents.get(serialNumber);
        if (conn == null || !conn.session().isOpen()) {
            log.debug("Impossible de récupérer les processus : agent non connecté pour UC={}", serialNumber);
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        String requestId = UUID.randomUUID().toString();
        CompletableFuture<List<AgentProcessInfo>> future = new CompletableFuture<>();
        pendingProcessRequests.put(requestId, future);

        future.whenComplete((res, ex) -> pendingProcessRequests.remove(requestId));

        try {
            JsonObject cmd = Json.createObjectBuilder()
                    .add("action", "GET_PROCESSES")
                    .add("requestId", requestId)
                    .build();
            conn.session().sendMessage(new TextMessage(cmd.toString()));
            return future
                    .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        if (ex instanceof TimeoutException || ex.getCause() instanceof TimeoutException) {
                            log.warn("Timeout lors de la récupération des processus pour UC={}", serialNumber);
                        } else {
                            log.error("Erreur lors de la récupération des processus pour UC={} : {}", serialNumber, ex.getMessage());
                        }
                        return Collections.emptyList();
                    });
        } catch (Exception e) {
            pendingProcessRequests.remove(requestId);
            log.error("Erreur lors de la demande de processus pour UC={} : {}", serialNumber, e.getMessage());
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }

    /**
     * Version synchrone d'interrogation de l'agent pour compatibilité.
     */
    public List<AgentProcessInfo> fetchProcesses(final String serialNumber, final long timeoutSeconds) {
        try {
            return fetchProcessesAsync(serialNumber, timeoutSeconds).join();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public AgentKillResult executeKill(final String serialNumber, final int pid, final long timeoutSeconds) {
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

    private void closeSessionQuietly(final WebSocketSession session, final CloseStatus status) {
        try {
            if (session != null && session.isOpen()) {
                session.close(status);
            }
        } catch (Exception e) {
            log.debug("Erreur lors de la fermeture de la session WebSocket : {}", e.getMessage());
        }
    }
}
