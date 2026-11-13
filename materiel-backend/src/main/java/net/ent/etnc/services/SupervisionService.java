package net.ent.etnc.services;

import java.util.List;

import net.ent.etnc.dtos.supervision.KillProcessRequest;
import net.ent.etnc.dtos.supervision.KillProcessResponse;
import net.ent.etnc.dtos.supervision.ProcessInfoDto;
import net.ent.etnc.dtos.supervision.WorkstationDto;

/**
 * Service de supervision des postes et des processus applicatifs.
 */
public interface SupervisionService {

    /**
     * Liste les postes de travail (tous ou filtrés par salle), en incluant les postes connectés.
     */
    List<WorkstationDto> findWorkstations(final Long roomId);

    /**
     * Récupère les processus applicatifs en cours d'exécution sur les postes demandés.
     */
    List<ProcessInfoDto> findProcessesForWorkstations(final List<Long> workstationIds);

    /**
     * Arrête un processus sur un poste et enregistre un KillEvent.
     */
    KillProcessResponse killProcess(final KillProcessRequest request);
}
