/**
 * Service de supervision des postes et des processus applicatifs en temps réel.
 * Communique avec le backend Spring Boot via /api/v1/supervision.
 * RG : RG-25 (processus lancés), RG-26 (arrêt en un clic), RG-28 (masterisation)
 */
import type {ProcessInfo, Workstation} from '../models/supervision.model.ts';
import {apiRequest} from './http-client.ts';

interface WorkstationDto {
    id: number;
    roomId: number | null;
    name: string;
    traineeLabel: string;
    macAddress: string;
    online: boolean;
    hostname?: string;
}

interface ProcessInfoDto {
    pid: number;
    workstationId: number;
    name: string;
    startedAt: string;
}

interface KillProcessResponseDto {
    success: boolean;
    message: string;
    killEventId?: number;
}

export const supervisionServices = {

    /**
     * Récupère la liste des postes de travail (tous les postes ou filtrés par salle).
     */
    findWorkstations: async (roomId?: number | null): Promise<Workstation[]> => {
        const endpoint = (roomId !== null && roomId !== undefined)
            ? `/supervision/rooms/${roomId}/workstations`
            : `/supervision/workstations`;
        const dtoList = await apiRequest<WorkstationDto[]>(endpoint);
        return dtoList.map(dto => ({
            id: dto.id,
            roomId: dto.roomId,
            name: dto.name,
            traineeLabel: dto.traineeLabel,
            macAddress: dto.macAddress,
            online: dto.online,
            hostname: dto.hostname,
            status: dto.online ? 'online' : 'offline',
        }));
    },

    /**
     * Récupère la liste des processus applicatifs actifs sur les postes donnés (RG-25).
     */
    findProcesses: async (workstationIds: number[]): Promise<ProcessInfo[]> => {
        if (workstationIds.length === 0) return [];
        const query = workstationIds.map(id => `workstationIds=${id}`).join('&');
        const dtoList = await apiRequest<ProcessInfoDto[]>(`/supervision/processes?${query}`);
        return dtoList.map(dto => ({
            pid: dto.pid,
            workstationId: dto.workstationId,
            name: dto.name,
            startedAt: new Date(dto.startedAt),
        }));
    },

    /**
     * Arrête un processus sur un poste (RG-26).
     */
    killProcess: async (process: ProcessInfo): Promise<void> => {
        const response = await apiRequest<KillProcessResponseDto>('/supervision/kill', {
            method: 'POST',
            body: JSON.stringify({
                workstationId: process.workstationId,
                pid: process.pid,
                processName: process.name,
            }),
        });
        if (!response.success) {
            throw new Error(response.message || 'Impossible d\'arrêter le processus sur le poste');
        }
    },

    /**
     * Masterisation d'un poste (RG-28).
     */
    reimage: async (workstation: Workstation): Promise<void> => {
        // En attente d'implémentation backend pour le déclenchement PXE/WDS
        console.warn(`Demande de masterisation envoyée pour ${workstation.name}`);
    },
};
