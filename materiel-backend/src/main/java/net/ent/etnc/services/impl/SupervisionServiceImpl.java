package net.ent.etnc.services.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.dtos.supervision.KillProcessRequest;
import net.ent.etnc.dtos.supervision.KillProcessResponse;
import net.ent.etnc.dtos.supervision.ProcessInfoDto;
import net.ent.etnc.dtos.supervision.WorkstationDto;
import net.ent.etnc.models.Application;
import net.ent.etnc.models.KillEvent;
import net.ent.etnc.models.Material;
import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.EMaterialType;
import net.ent.etnc.repositories.MaterialRepository;
import net.ent.etnc.services.ApplicationService;
import net.ent.etnc.services.KillEventService;
import net.ent.etnc.services.SupervisionService;
import net.ent.etnc.services.UserService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import net.ent.etnc.services.commons.ServiceException;
import net.ent.etnc.websocket.AgentWebSocketHandler;

/**
 * Implémentation du service de supervision.
 */
@Slf4j
@Service
public class SupervisionServiceImpl implements SupervisionService {

    private final MaterialRepository materialRepository;
    private final AgentWebSocketHandler agentWebSocketHandler;
    private final KillEventService killEventService;
    private final UserService userService;
    private final ApplicationService applicationService;

    @Autowired
    public SupervisionServiceImpl(final MaterialRepository materialRepository, final AgentWebSocketHandler agentWebSocketHandler, final KillEventService killEventService,
            final UserService userService, final ApplicationService applicationService) {
        this.materialRepository = materialRepository;
        this.agentWebSocketHandler = agentWebSocketHandler;
        this.killEventService = killEventService;
        this.userService = userService;
        this.applicationService = applicationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkstationDto> findWorkstations(final Long roomId) {
        List<Material> ucs = (roomId != null) ?
                materialRepository.findByRoomIdAndMaterialTypeAndArchivedFalse(roomId, EMaterialType.UC) :
                materialRepository.findByMaterialTypeAndArchivedFalse(EMaterialType.UC);

        List<WorkstationDto> workstations = new ArrayList<>();
        Set<String> seenSerials = new HashSet<>();

        int index = 1;
        for (Material uc : ucs) {
            seenSerials.add(uc.getSerialNumber());
            boolean online = agentWebSocketHandler.isConnected(uc.getSerialNumber());
            Optional<AgentWebSocketHandler.AgentConnection> conn = agentWebSocketHandler.getConnection(uc.getSerialNumber());
            String macAddress = conn.map(AgentWebSocketHandler.AgentConnection::macAddress).filter(mac -> !mac.isBlank()).orElse("N/A");
            String hostname = conn.map(AgentWebSocketHandler.AgentConnection::hostname).filter(h -> !h.isBlank()).orElse(null);
            String username = conn.map(AgentWebSocketHandler.AgentConnection::username).filter(u -> !u.isBlank()).orElse(null);
            String os = conn.map(AgentWebSocketHandler.AgentConnection::os).filter(o -> !o.isBlank()).orElse(null);

            String name = (hostname != null && !hostname.isBlank()) ? hostname + " (" + uc.getSerialNumber() + ")" : "Poste " + index + " (" + uc.getSerialNumber() + ")";
            String traineeLabel = (username != null && !username.isBlank()) ? username : (online ? "Poste connecté" : "Poste éteint / Agent déconnecté");

            workstations.add(new WorkstationDto(uc.getId(), uc.getRoom() != null ? uc.getRoom().getId() : null, name, traineeLabel, macAddress, online, hostname, username, os));
            index++;
        }

        // Si aucun filtre de salle n'est demandé, inclure tous les agents connectés même s'ils n'ont pas encore de salle
        if (roomId == null) {
            for (AgentWebSocketHandler.AgentConnection agent : agentWebSocketHandler.getAllConnectedAgents()) {
                if (!seenSerials.contains(agent.serialNumber())) {
                    Material uc = materialRepository.findBySerialNumber(agent.serialNumber()).orElse(null);
                    if (uc != null) {
                        seenSerials.add(agent.serialNumber());
                        String username = (agent.username() != null && !agent.username().isBlank()) ? agent.username() : null;
                        String traineeLabel = (username != null && !username.isBlank()) ? username : "Poste connecté";
                        workstations.add(new WorkstationDto(uc.getId(), null,
                                (agent.hostname() != null && !agent.hostname().isBlank() ? agent.hostname() : "Poste") + " (" + agent.serialNumber() + ")", traineeLabel,
                                agent.macAddress(), true, agent.hostname(), username, agent.os()));
                    }
                }
            }
        }

        // Trier avec les postes connectés en premier
        workstations.sort(Comparator.comparing(WorkstationDto::online).reversed().thenComparing(WorkstationDto::name));

        return workstations;
    }

    /**
     * Récupère les processus pour la liste des postes spécifiés.
     * Les appels réseau vers les agents sont exécutés en parallèle hors transaction SQL avec un délai global (Point I5).
     */
    @Override
    public List<ProcessInfoDto> findProcessesForWorkstations(final List<Long> workstationIds) {
        if (workstationIds == null || workstationIds.isEmpty()) {
            return List.of();
        }

        Set<Long> uniqueIds = new LinkedHashSet<>(workstationIds);
        List<Material> ucs = new ArrayList<>();
        for (Long ucId : uniqueIds) {
            materialRepository.findById(ucId).ifPresent(ucs::add);
        }

        record WorkstationQuery(Material uc, CompletableFuture<List<AgentWebSocketHandler.AgentProcessInfo>> future) {}

        List<WorkstationQuery> queries = new ArrayList<>();
        for (Material uc : ucs) {
            if (!EMaterialType.UC.equals(uc.getMaterialType())) {
                continue;
            }
            CompletableFuture<List<AgentWebSocketHandler.AgentProcessInfo>> future =
                    agentWebSocketHandler.fetchProcessesAsync(uc.getSerialNumber(), 3);
            queries.add(new WorkstationQuery(uc, future));
        }

        if (queries.isEmpty()) {
            return List.of();
        }

        try {
            CompletableFuture.allOf(queries.stream().map(WorkstationQuery::future).toArray(CompletableFuture[]::new))
                    .orTimeout(4, TimeUnit.SECONDS)
                    .join();
        } catch (Exception e) {
            log.warn("Délai global d'interrogation des agents de supervision dépassé : {}", e.getMessage());
        }

        User currentUser = null;
        try {
            currentUser = userService.currentUser();
        } catch (Exception e) {
            log.debug("Impossible de récupérer l'utilisateur courant pour l'auto-kill : {}", e.getMessage());
        }

        List<ProcessInfoDto> allProcesses = new ArrayList<>();

        for (WorkstationQuery query : queries) {
            Material uc = query.uc();
            List<AgentWebSocketHandler.AgentProcessInfo> agentProcs;
            try {
                agentProcs = query.future().getNow(Collections.emptyList());
            } catch (Exception e) {
                agentProcs = Collections.emptyList();
            }

            for (AgentWebSocketHandler.AgentProcessInfo proc : agentProcs) {
                // Enregistrement / découverte de l'application en base si nouvelle
                Application app = applicationService.registerDiscoveredApplication(proc.name(), proc.icon());

                // Auto-kill si l'application est sur liste noire
                if (app != null && app.isBlacklisted()) {
                    log.warn("Processus interdit détecté : {} (PID={}) sur poste {}. Déclenchement arrêt automatique...", proc.name(), proc.pid(), uc.getSerialNumber());

                    AgentWebSocketHandler.AgentKillResult killRes = agentWebSocketHandler.executeKill(uc.getSerialNumber(), proc.pid(), 5);

                    // Journalisation dans KillEvent
                    try {
                        KillEvent killEvent = new KillEvent();
                        killEvent.setUc(uc);
                        killEvent.setProcessName(proc.name());
                        killEvent.setKillDateTime(LocalDateTime.now());
                        killEvent.setUser(currentUser);
                        killEvent.setResult(killRes.success());
                        killEventService.create(killEvent);
                    } catch (Exception e) {
                        log.error("Échec de l'enregistrement de l'événement KillEvent pour auto-kill : {}", e.getMessage());
                    }

                    // Ne pas renvoyer le processus au front puisqu'il a été arrêté
                    continue;
                }

                allProcesses.add(new ProcessInfoDto(proc.pid(), uc.getId(), proc.name(),
                        Instant.ofEpochMilli(proc.startedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                        proc.icon()));
            }
        }

        return allProcesses;
    }

    @Override
    @Transactional
    public KillProcessResponse killProcess(final KillProcessRequest request) {
        Material uc = materialRepository.findById(request.workstationId())
                .orElseThrow(() -> new ResourceNotFoundException("Unité centrale non trouvée avec l'id : " + request.workstationId()));

        if (!EMaterialType.UC.equals(uc.getMaterialType())) {
            throw new ServiceException("Le matériel ciblé n'est pas une unité centrale (UC)");
        }

        AgentWebSocketHandler.AgentKillResult killResult = agentWebSocketHandler.executeKill(uc.getSerialNumber(), request.pid(), 5);

        KillEvent killEvent = new KillEvent();
        killEvent.setUc(uc);
        killEvent.setProcessName(request.processName());
        killEvent.setKillDateTime(LocalDateTime.now());
        killEvent.setUser(userService.currentUser());
        killEvent.setResult(killResult.success());

        KillEvent savedEvent = killEventService.create(killEvent);

        if (!killResult.success()) {
            return new KillProcessResponse(false, killResult.error() != null ? killResult.error() : "L'agent n'a pas pu arrêter le processus", savedEvent.getId());
        }

        return new KillProcessResponse(true, "Processus arrêté avec succès", savedEvent.getId());
    }
}
