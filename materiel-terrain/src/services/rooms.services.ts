/**
 * Accès en lecture à la ressource /api/v1/rooms (le bâtiment est imbriqué dans chaque salle).
 * Lève ApiError si l'API répond en erreur ou ne répond pas.
 */
import type {Room} from '../models/room.model.ts';
import {type RoomDto, roomFromDto} from '../models/room.dto.ts';
import {type PageDto, pageContent} from '../models/page.dto.ts';
import {apiRequest} from './http-client.ts';

export const roomsServices = {

    /**
     * Liste toutes les salles, triées par bâtiment puis par numéro.
     * TODO(perf): une seule page large ; à paginer si le nombre de salles dépasse 1000.
     */
    findAll: async (): Promise<Room[]> => {
        const page = await apiRequest<PageDto<RoomDto>>('/rooms?size=1000&sort=building.number&sort=number');
        return pageContent(page).map(roomFromDto);
    },
};
