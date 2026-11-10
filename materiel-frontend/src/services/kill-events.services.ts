/**
 * Accès à la ressource /api/v1/killevents : arrêts d'applications sur les postes (UC), en lecture.
 * Les arrêts sont enregistrés par le backend lui-même, à chaque POST /api/v1/supervision/kill.
 * Toutes les méthodes lèvent ApiError si l'API répond en erreur.
 * Utilisé par : features/supervision
 * RG : RG-26 (arrêt enregistré) ; journal demandé en entretien 4
 */
import type {SupervisionEvent} from '../models/supervision.model.ts';
import {type KillEventDto, killEventFromDto} from '../models/kill-event.dto.ts';
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
};
