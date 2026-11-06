/**
 * Accès à la ressource /api/v1/sightings : constats de terrain partagés entre les téléphones.
 * Toutes les méthodes lèvent ApiError si l'API répond en erreur ou ne répond pas.
 */
import type {SightingDto, SightingRequestDto} from '../models/sighting.dto.ts';
import {apiRequest} from './http-client.ts';

/** Ramène les fractions de seconde à 3 chiffres : le serveur en envoie 6, que Safari peut refuser. */
function toBrowserIso(iso: string): string {
    return iso.replace(/(\.\d{3})\d+/, '$1');
}

export const sightingsServices = {

    /** Tous les constats (tout utilisateur connecté). */
    findAll: async (): Promise<SightingDto[]> => {
        const list = await apiRequest<SightingDto[]>('/sightings');
        return list.map((dto) => ({...dto, seenAt: toBrowserIso(dto.seenAt)}));
    },

    /** Enregistre un scan dans une salle (tout utilisateur connecté). Lève ApiError 404 si la salle n'existe plus. */
    record: async (request: SightingRequestDto): Promise<void> => {
        await apiRequest<SightingDto>('/sightings', {method: 'POST', body: JSON.stringify(request)});
    },

    /** Efface les constats d'une salle, pour tous (MANAGER, ADMIN). */
    clearRoom: async (roomId: number): Promise<void> => {
        await apiRequest<void>(`/sightings?roomId=${roomId}`, {method: 'DELETE'});
    },
};
