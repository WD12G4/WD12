/**
 * Forme JSON échangée avec /api/v1/killevents et conversion vers le journal de supervision.
 * Source : KillEventRequest, KillEventResponse (backend, package dtos.killevent).
 * RG : RG-26 (arrêt d'une application) ; journal demandé en entretien 4
 */
import type {MaterialDto} from './material.dto.ts';
import type {SupervisionEvent} from './supervision.model.ts';
import {parseApiDateTime} from './date-time.dto.ts';

/** Réponse de l'API pour un arrêt d'application. */
export interface KillEventDto {
    id: number;
    version: number;
    processName: string;
    /** UC sur laquelle l'application a été arrêtée, avec sa salle. */
    uc: MaterialDto;
    killDateTime: string;
    /** Vrai si l'arrêt a réussi. */
    result: boolean;
}

/** Corps attendu par POST /api/v1/killevents. */
export interface KillEventRequestDto {
    processName: string;
    ucId: number;
    /** Date-heure de l'arrêt, ni future ni absente (le backend le vérifie). */
    killDateTime: string;
    result: boolean;
}

/**
 * Convertit un arrêt d'application en événement du journal.
 * @param dto arrêt reçu de l'API
 */
export function killEventFromDto(dto: KillEventDto): SupervisionEvent {
    return {
        id: `kill-${dto.id}`,
        roomId: dto.uc.room?.id ?? null,
        workstationName: dto.uc.serialNumber,
        kind: 'kill',
        processName: dto.processName,
        at: parseApiDateTime(dto.killDateTime),
    };
}
