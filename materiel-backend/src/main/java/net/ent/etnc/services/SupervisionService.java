package net.ent.etnc.services;

import net.ent.etnc.dtos.supervision.KillProcessRequest;
import net.ent.etnc.dtos.supervision.KillProcessResponse;
import net.ent.etnc.dtos.supervision.ProcessInfoDto;
import net.ent.etnc.dtos.supervision.WorkstationDto;

import java.util.List;

/**
 * Service de supervision des postes et des processus applicatifs.
 */
public interface SupervisionService {

    /**
     * Liste les postes de travail (matériels de type UC) situés dans une salle.
     */
    List<WorkstationDto> findWorkstationsByRoom(Long roomId);

    /**
     * Liste les postes de travail (tous ou filtrés par salle), en incluant les postes connectés.
     */
    List<WorkstationDto> findWorkstations(Long roomId);

    /**
     * Récupère les processus applicatifs en cours d'exécution sur les postes demandés.
     */
    List<ProcessInfoDto> findProcessesForWorkstations(List<Long> workstationIds);

    /**
     * Arrête un processus sur un poste et enregistre un KillEvent.
     */
    KillProcessResponse killProcess(KillProcessRequest request);
}
