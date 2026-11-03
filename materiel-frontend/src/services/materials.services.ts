/**
 * Accès à la ressource /api/v1/materials.
 * Toutes les méthodes lèvent ApiError si l'API répond en erreur.
 * RG : RG-07 (affectation / remise en stock), RG-10 (archivage), RG-16, RG-17 (droits)
 */
import type {Material, MaterialInput} from '../models/material.model.ts';
import {
    type MaterialAssignRequestDto, type MaterialDto, materialFromDto, materialToRequest,
} from '../models/material.dto.ts';
import {type PageDto, pageContent} from '../models/page.dto.ts';
import {apiRequest} from './http-client.ts';

export const materialsServices = {

    /**
     * Liste tous les matériels, triés par numéro de série.
     * L'API ne filtre pas : le filtrage se fait côté front (use-material-filters.ts).
     * TODO(perf): une seule page large ; à paginer ou filtrer côté backend au-delà de 1000 matériels.
     */
    findAll: async (): Promise<Material[]> => {
        const page = await apiRequest<PageDto<MaterialDto>>('/materials?size=1000&sort=serialNumber');
        return pageContent(page).map(materialFromDto);
    },

    /** Récupère un matériel par son identifiant. Lève ApiError 404 s'il n'existe pas. */
    findById: async (id: number): Promise<Material> => {
        const dto = await apiRequest<MaterialDto>(`/materials/${id}`);
        return materialFromDto(dto);
    },

    /** Crée un matériel (ADMIN). Lève ApiError 400 si une règle métier est violée (RG-01, RG-05, RG-06). */
    create: async (input: MaterialInput): Promise<Material> => {
        const dto = await apiRequest<MaterialDto>('/materials', {
            method: 'POST',
            body: JSON.stringify(materialToRequest(input)),
        });
        return materialFromDto(dto);
    },

    /** Modifie un matériel (ADMIN). */
    update: async (id: number, input: MaterialInput): Promise<Material> => {
        const dto = await apiRequest<MaterialDto>(`/materials/${id}`, {
            method: 'PUT',
            body: JSON.stringify(materialToRequest(input)),
        });
        return materialFromDto(dto);
    },

    /**
     * Affecte le matériel à une salle, ou le remet en stock si roomId vaut null (RG-07).
     * Réservé à MANAGER et ADMIN (RG-17). Refusé si le matériel est archivé (RG-06).
     */
    assignRoom: async (id: number, roomId: number | null): Promise<Material> => {
        const body: MaterialAssignRequestDto = {roomId};
        const dto = await apiRequest<MaterialDto>(`/materials/${id}/room`, {
            method: 'PATCH',
            body: JSON.stringify(body),
        });
        return materialFromDto(dto);
    },

    /** Archive le matériel : suppression logique, la salle est libérée (RG-10). Réservé à ADMIN. */
    delete: async (id: number): Promise<void> => {
        await apiRequest<void>(`/materials/${id}`, {method: 'DELETE'});
    },
};
