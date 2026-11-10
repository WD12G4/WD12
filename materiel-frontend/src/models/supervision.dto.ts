/**
 * Forme JSON échangée avec /api/v1/supervision et conversions vers le modèle.
 * Source : WorkstationDto, ProcessInfoDto, KillProcessRequest, KillProcessResponse
 * (backend, package dtos.supervision).
 */
import type {ProcessInfo, Workstation} from './supervision.model.ts';
import {parseApiDateTime} from './date-time.dto.ts';

/** Valeur renvoyée par le backend quand l'adresse MAC est inconnue (agent déconnecté). */
const UNKNOWN_MAC = 'N/A';

/** Réponse de l'API pour un poste (une UC de la salle). */
export interface WorkstationDto {
    id: number;
    roomId: number | null;
    name: string;
    /** Rempli par le backend avec l'état de l'agent (« Poste connecté »…) : non utilisé, voir online. */
    traineeLabel: string;
    macAddress: string;
    online: boolean;
    hostname: string | null;
}

/** Réponse de l'API pour un processus. */
export interface ProcessInfoDto {
    pid: number;
    workstationId: number;
    name: string;
    startedAt: string;
}

/** Corps attendu par POST /api/v1/supervision/kill. */
export interface KillProcessRequestDto {
    workstationId: number;
    pid: number;
    processName: string;
}

/** Réponse de POST /api/v1/supervision/kill (le KillEvent est enregistré dans tous les cas). */
export interface KillProcessResponseDto {
    success: boolean;
    message: string;
    killEventId: number | null;
}

/**
 * Convertit un poste reçu de l'API.
 * @param dto poste reçu
 */
export function workstationFromDto(dto: WorkstationDto): Workstation {
    return {
        id: dto.id,
        roomId: dto.roomId,
        name: dto.name,
        hostname: dto.hostname || null,
        macAddress: dto.macAddress && dto.macAddress !== UNKNOWN_MAC ? dto.macAddress : null,
        status: dto.online ? 'online' : 'offline',
    };
}

/**
 * Convertit un processus reçu de l'API.
 * @param dto processus reçu
 */
export function processFromDto(dto: ProcessInfoDto): ProcessInfo {
    return {...dto, startedAt: parseApiDateTime(dto.startedAt)};
}
