/**
 * Accès à la ressource /api/v1/rooms.
 * Toutes les méthodes lèvent ApiError si l'API répond en erreur.
 * RG : RG-13, RG-31 (création, modification, suppression réservées à l'ADMIN)
 */
import type {Room, RoomInput} from '../models/room.model.ts';
import {type RoomDto, roomFromDto, roomToRequest} from '../models/room.dto.ts';
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

    /** Récupère une salle. Lève ApiError 404 si elle n'existe pas. */
    findById: async (id: number): Promise<Room> => {
        const dto = await apiRequest<RoomDto>(`/rooms/${id}`);
        return roomFromDto(dto);
    },

    /** Crée une salle (ADMIN). Lève ApiError 400 si le numéro est invalide ou déjà pris dans ce bâtiment. */
    create: async (input: RoomInput): Promise<Room> => {
        const dto = await apiRequest<RoomDto>('/rooms', {
            method: 'POST',
            body: JSON.stringify(roomToRequest(input)),
        });
        return roomFromDto(dto);
    },

    /** Modifie une salle (ADMIN). */
    update: async (id: number, input: RoomInput): Promise<Room> => {
        const dto = await apiRequest<RoomDto>(`/rooms/${id}`, {
            method: 'PUT',
            body: JSON.stringify(roomToRequest(input)),
        });
        return roomFromDto(dto);
    },

    /** Supprime une salle (ADMIN). Suppression définitive côté backend, pas un archivage. */
    delete: async (id: number): Promise<void> => {
        await apiRequest<void>(`/rooms/${id}`, {method: 'DELETE'});
    },
};
