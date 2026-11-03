/**
 * Accès à la ressource /api/v1/buildings.
 * Toutes les méthodes lèvent ApiError si l'API répond en erreur.
 * RG : RG-12, RG-31 (création, modification, suppression réservées à l'ADMIN)
 */
import type {Building, BuildingInput} from '../models/building.model.ts';
import {type BuildingDto, buildingFromDto, buildingToRequest} from '../models/building.dto.ts';
import {type PageDto, pageContent} from '../models/page.dto.ts';
import {apiRequest} from './http-client.ts';

export const buildingsServices = {

    /**
     * Liste tous les bâtiments, triés par numéro.
     * TODO(perf): une seule page large ; à paginer si le nombre de bâtiments dépasse 1000.
     */
    findAll: async (): Promise<Building[]> => {
        const page = await apiRequest<PageDto<BuildingDto>>('/buildings?size=1000&sort=number');
        return pageContent(page).map(buildingFromDto);
    },

    /** Récupère un bâtiment. Lève ApiError 404 s'il n'existe pas. */
    findById: async (id: number): Promise<Building> => {
        const dto = await apiRequest<BuildingDto>(`/buildings/${id}`);
        return buildingFromDto(dto);
    },

    /** Crée un bâtiment (ADMIN). Lève ApiError 400 si le numéro est invalide ou déjà pris. */
    create: async (input: BuildingInput): Promise<Building> => {
        const dto = await apiRequest<BuildingDto>('/buildings', {
            method: 'POST',
            body: JSON.stringify(buildingToRequest(input)),
        });
        return buildingFromDto(dto);
    },

    /** Modifie un bâtiment (ADMIN). */
    update: async (id: number, input: BuildingInput): Promise<Building> => {
        const dto = await apiRequest<BuildingDto>(`/buildings/${id}`, {
            method: 'PUT',
            body: JSON.stringify(buildingToRequest(input)),
        });
        return buildingFromDto(dto);
    },

    /** Supprime un bâtiment (ADMIN). Suppression définitive côté backend, pas un archivage. */
    delete: async (id: number): Promise<void> => {
        await apiRequest<void>(`/buildings/${id}`, {method: 'DELETE'});
    },
};
