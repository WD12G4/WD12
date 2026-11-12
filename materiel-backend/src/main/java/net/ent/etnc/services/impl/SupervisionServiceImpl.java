package net.ent.etnc.services.impl;

import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.dtos.supervision.KillProcessRequest;
import net.ent.etnc.dtos.supervision.KillProcessResponse;
import net.ent.etnc.dtos.supervision.ProcessInfoDto;
import net.ent.etnc.dtos.supervision.WorkstationDto;
import net.ent.etnc.models.KillEvent;
import net.ent.etnc.models.Material;
import net.ent.etnc.models.enums.EMaterialType;
import net.ent.etnc.repositories.MaterialRepository;
import net.ent.etnc.services.KillEventService;
import net.ent.etnc.services.SupervisionService;
import net.ent.etnc.services.UserService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import net.ent.etnc.services.commons.ServiceException;
import net.ent.etnc.websocket.AgentWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

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

    @Autowired
    public SupervisionServiceImpl(MaterialRepository materialRepository,
                                  AgentWebSocketHandler agentWebSocketHandler,
                                  KillEventService killEventService,
                                  UserService userService) {
        this.materialRepository = materialRepository;
        this.agentWebSocketHandler = agentWebSocketHandler;
        this.killEventService = killEventService;
        this.userService = userService;
    }

    @Override
    public List<WorkstationDto> findWorkstationsByRoom(Long roomId) {
        return findWorkstations(roomId);
    }

    @Override
    @Transactional
    public List<WorkstationDto> findWorkstations(Long roomId) {
        // Auto-découverte : pour chaque agent connecté via WebSocket, s'assurer qu'il a une fiche matériel en base
        for (AgentWebSocketHandler.AgentConnection agent : agentWebSocketHandler.getAllConnectedAgents()) {
            if (materialRepository.findBySerialNumber(agent.serialNumber()).isEmpty()) {
                Material newUc = new Material();
                newUc.setSerialNumber(agent.serialNumber());
                newUc.setMaterialType(EMaterialType.UC);
                newUc.setEndAvailabilityDate(LocalDate.now().plusYears(5));
                newUc.setArchived(false);
                materialRepository.save(newUc);
                log.info("Nouveau matériel UC auto-découvert et enregistré en base [serialNumber={}]", agent.serialNumber());
            }
        }

        List<Material> ucs = (roomId != null)
                ? materialRepository.findByRoomIdAndMaterialTypeAndArchivedFalse(roomId, EMaterialType.UC)
                : materialRepository.findByMaterialTypeAndArchivedFalse(EMaterialType.UC);

        List<WorkstationDto> workstations = new ArrayList<>();
        Set<String> seenSerials = new HashSet<>();

        int index = 1;
        for (Material uc : ucs) {
            seenSerials.add(uc.getSerialNumber());
            boolean online = agentWebSocketHandler.isConnected(uc.getSerialNumber());
            Optional<AgentWebSocketHandler.AgentConnection> conn = agentWebSocketHandler.getConnection(uc.getSerialNumber());
            String macAddress = conn.map(AgentWebSocketHandler.AgentConnection::macAddress)
                    .filter(mac -> !mac.isBlank())
                    .orElse("N/A");
            String hostname = conn.map(AgentWebSocketHandler.AgentConnection::hostname)
                    .filter(h -> !h.isBlank())
                    .orElse(null);
            String username = conn.map(AgentWebSocketHandler.AgentConnection::username)
                    .filter(u -> !u.isBlank())
                    .orElse(null);
            String os = conn.map(AgentWebSocketHandler.AgentConnection::os)
                    .filter(o -> !o.isBlank())
                    .orElse(null);

            String name = (hostname != null && !hostname.isBlank())
                    ? hostname + " (" + uc.getSerialNumber() + ")"
                    : "Poste " + index + " (" + uc.getSerialNumber() + ")";
            String traineeLabel = (username != null && !username.isBlank())
                    ? username
                    : (online ? "Poste connecté" : "Poste éteint / Agent déconnecté");

            workstations.add(new WorkstationDto(
                    uc.getId(),
                    uc.getRoom() != null ? uc.getRoom().getId() : null,
                    name,
                    traineeLabel,
                    macAddress,
                    online,
                    hostname,
                    username,
                    os
            ));
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
                        workstations.add(new WorkstationDto(
                                uc.getId(),
                                null,
                                (agent.hostname() != null && !agent.hostname().isBlank() ? agent.hostname() : "Poste") + " (" + agent.serialNumber() + ")",
                                traineeLabel,
                                agent.macAddress(),
                                true,
                                agent.hostname(),
                                username,
                                agent.os()
                        ));
                    }
                }
            }
        }

        // Trier avec les postes connectés en premier
        workstations.sort(Comparator.comparing(WorkstationDto::online).reversed()
                .thenComparing(WorkstationDto::name));

        return workstations;
    }

    @Override
    public List<ProcessInfoDto> findProcessesForWorkstations(List<Long> workstationIds) {
        if (workstationIds == null || workstationIds.isEmpty()) {
            return List.of();
        }

        List<ProcessInfoDto> allProcesses = new ArrayList<>();

        for (Long ucId : workstationIds) {
            Optional<Material> ucOpt = materialRepository.findById(ucId);
            if (ucOpt.isEmpty()) {
                continue;
            }
            Material uc = ucOpt.get();

            List<AgentWebSocketHandler.AgentProcessInfo> agentProcs =
                    agentWebSocketHandler.fetchProcesses(uc.getSerialNumber(), 3);

            for (AgentWebSocketHandler.AgentProcessInfo proc : agentProcs) {
                LocalDateTime startedAt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(proc.startedAt()),
                    ZoneId.systemDefault()
                );
                allProcesses.add(new ProcessInfoDto(
                        proc.pid(),
                        uc.getId(),
                        proc.name(),
                        startedAt
                ));
            }
        }

        return allProcesses;
    }

    @Override
    public KillProcessResponse killProcess(KillProcessRequest request) {
        Material uc = materialRepository.findById(request.workstationId())
                .orElseThrow(() -> new ResourceNotFoundException("Poste introuvable avec l'id : " + request.workstationId()));

        if (!EMaterialType.UC.equals(uc.getMaterialType())) {
            throw new ServiceException("Le matériel sélectionné n'est pas une unité centrale.");
        }

        log.info("Demande d'arrêt du processus [PID={}, nom={}, UC={}]",
                request.pid(), request.processName(), uc.getSerialNumber());

        AgentWebSocketHandler.AgentKillResult result =
                agentWebSocketHandler.executeKill(uc.getSerialNumber(), request.pid(), 5);

        // Nom effectif du processus arrêté ou visé
        String processName = (result.processName() != null && !result.processName().isBlank())
                ? result.processName()
                : request.processName();

        // Enregistrement systématique du KillEvent en base de données (résultat réussi ou échoué)
        KillEvent killEvent = new KillEvent();
        killEvent.setProcessName(processName);
        killEvent.setUc(uc);
        killEvent.setUser(this.userService.currentUser());
        killEvent.setKillDateTime(LocalDateTime.now());
        killEvent.setResult(result.success());

        KillEvent saved = killEventService.create(killEvent);
        log.info("KillEvent enregistré avec succès [id={}, success={}]", saved.getId(), result.success());

        if (!result.success()) {
            String errorMsg = (result.error() != null && !result.error().isBlank())
                    ? result.error()
                    : "Impossible d'arrêter le processus sur le poste client";
            log.warn("Échec de l'arrêt du processus sur l'agent : {}", errorMsg);
            return new KillProcessResponse(false, errorMsg, saved.getId());
        }

        return new KillProcessResponse(true, "Processus arrêté avec succès", saved.getId());
    }
}
