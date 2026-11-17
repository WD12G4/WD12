package net.ent.etnc.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.dtos.supervision.KillProcessRequest;
import net.ent.etnc.dtos.supervision.KillProcessResponse;
import net.ent.etnc.dtos.supervision.ProcessInfoDto;
import net.ent.etnc.dtos.supervision.WorkstationDto;
import net.ent.etnc.services.SupervisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la supervision des postes et processus en temps réel.
 */
@RestController
@RequestMapping("/api/v1/supervision")
public class SupervisionController {

    private final SupervisionService supervisionService;

    @Autowired
    public SupervisionController(final SupervisionService supervisionService) {
        this.supervisionService = supervisionService;
    }

    /**
     * Récupère la liste des postes de travail (UC) pour une salle donnée.
     */
    @GetMapping("/rooms/{roomId}/workstations")
    public List<WorkstationDto> getWorkstationsByRoom(@PathVariable final Long roomId) {
        return this.supervisionService.findWorkstations(roomId);
    }

    /**
     * Récupère la liste des processus applicatifs actifs sur une liste de postes.
     */
    @GetMapping("/processes")
    public List<ProcessInfoDto> getProcesses(@RequestParam(name = "workstationIds", required = false) final List<Long> workstationIds) {
        return this.supervisionService.findProcessesForWorkstations(workstationIds);
    }

    /**
     * Demande l'arrêt d'un processus utilisateur sur un poste supervisé.
     */
    @PostMapping("/kill")
    public KillProcessResponse killProcess(@Valid @RequestBody final KillProcessRequest request) {
        return this.supervisionService.killProcess(request);
    }
}
