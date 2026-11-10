/**
 * Accès à la ressource /api/v1/supervision : postes d'une salle, processus remontés par les agents
 * de supervision, arrêt d'un processus. Le backend enregistre lui-même chaque arrêt (KillEvent).
 * Toutes les méthodes lèvent ApiError si l'API répond en erreur.
 * Utilisé par : features/supervision
 * RG : RG-25 (processus lancés), RG-26 (arrêt en un clic)
 */
import type {ProcessInfo, Workstation} from '../models/supervision.model.ts';
import {
    type KillProcessRequestDto, type KillProcessResponseDto, type ProcessInfoDto, processFromDto,
    type WorkstationDto, workstationFromDto,
} from '../models/supervision.dto.ts';
import {ApiError} from './api-error.ts';
import {apiRequest} from './http-client.ts';

/**
 * Code d'erreur utilisé quand l'agent refuse ou n'arrive pas à arrêter le processus : 400, comme les
 * refus métier, pour que le message de l'agent soit affiché tel quel (voir api-error.ts).
 */
const KILL_REFUSED_STATUS = 400;

export const supervisionServices = {

    /** Postes d'une salle : ses UC, en ligne si leur agent est connecté. */
    findWorkstations: async (roomId: number): Promise<Workstation[]> => {
        const dtos = await apiRequest<WorkstationDto[]>(`/supervision/rooms/${roomId}/workstations`);
        return dtos.map(workstationFromDto);
    },

    /** Processus lancés par l'utilisateur sur les postes donnés (RG-25). */
    findProcesses: async (workstationIds: number[]): Promise<ProcessInfo[]> => {
        if (workstationIds.length === 0) return [];
        const query = workstationIds.map(id => `workstationIds=${id}`).join('&');
        const dtos = await apiRequest<ProcessInfoDto[]>(`/supervision/processes?${query}`);
        return dtos.map(processFromDto);
    },

    /**
     * Arrête un processus (RG-26). Lève ApiError 400, avec le message de l'agent, s'il refuse
     * (processus protégé) ou n'y arrive pas ; l'arrêt échoué est tout de même inscrit au journal.
     */
    killProcess: async (process: ProcessInfo): Promise<void> => {
        const body: KillProcessRequestDto = {workstationId: process.workstationId, pid: process.pid, processName: process.name};
        const response = await apiRequest<KillProcessResponseDto>('/supervision/kill', {method: 'POST', body: JSON.stringify(body)});
        if (!response.success) throw new ApiError(KILL_REFUSED_STATUS, `${process.name} : ${response.message}`);
    },
};
