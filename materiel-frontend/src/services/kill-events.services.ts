/**
 * Accès à la ressource /api/v1/killevents : arrêts d'applications sur les postes (UC).
 * Toutes les méthodes lèvent ApiError si l'API répond en erreur.
 * Utilisé par : features/supervision
 * RG : RG-26 (arrêt enregistré) ; journal demandé en entretien 4
 */
import type {SupervisionEvent} from '../models/supervision.model.ts';
import {type KillEventDto, killEventFromDto, type KillEventRequestDto} from '../models/kill-event.dto.ts';
import {toApiDateTime} from '../models/date-time.dto.ts';
import {type PageDto, pageContent} from '../models/page.dto.ts';
import {apiRequest} from './http-client.ts';

export const killEventsServices = {
    /**
     * Arrêts d'applications d'une salle, du plus récent au plus ancien.
     * TODO(perf): l'API ne filtre pas par salle ; les 500 derniers arrêts sont filtrés côté front.
     */
    findByRoom: async (roomId: number): Promise<SupervisionEvent[]> => {
        const page = await apiRequest<PageDto<KillEventDto>>('/killevents?size=500&sort=killDateTime,desc');
        return pageContent(page).map(killEventFromDto).filter(event => event.roomId === roomId);
    },

    /** Enregistre l'arrêt réussi d'une application sur une UC, maintenant (RG-26). */
    create: async (processName: string, ucId: number): Promise<void> => {
        const body: KillEventRequestDto = {processName, ucId, killDateTime: toApiDateTime(new Date()), result: true};
        await apiRequest<KillEventDto>('/killevents', {method: 'POST', body: JSON.stringify(body)});
    },
};
