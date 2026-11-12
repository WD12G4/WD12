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
    /** Vrai si l'arrêt a réussi ; null pour un arrêt enregistré avant ce champ (réussi). */
    result: boolean | null;
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
        succeeded: dto.result ?? true,
        at: parseApiDateTime(dto.killDateTime),
        simulated: false,
        detail: null,
    };
}
