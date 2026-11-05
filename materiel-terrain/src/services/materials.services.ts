/**
 * Accès à la ressource /api/v1/materials, limité à ce dont le terrain a besoin.
 * Toutes les méthodes lèvent ApiError si l'API répond en erreur ou ne répond pas.
 * RG : RG-07 (affectation), RG-17 (droits MANAGER et ADMIN côté backend)
 */
import type {Material} from '../models/material.model.ts';
import {
    type MaterialAssignRequestDto, type MaterialDto, type MaterialRequestDto, materialFromDto,
} from '../models/material.dto.ts';
import {type PageDto, pageContent} from '../models/page.dto.ts';
import {apiRequest} from './http-client.ts';

export const materialsServices = {

    /**
     * Liste tout le parc (archivés compris), gardé ensuite sur le téléphone pour identifier sans réseau.
     * L'API ne propose pas de recherche par numéro de série : l'identification se fait sur cette liste.
     * TODO(perf): une seule page large ; à paginer au-delà de 1000 matériels.
     */
    findAll: async (): Promise<Material[]> => {
        const page = await apiRequest<PageDto<MaterialDto>>('/materials?size=1000&sort=serialNumber');
        return pageContent(page).map(materialFromDto);
    },

    /** Prend en compte un matériel non recensé (MANAGER, ADMIN). Lève ApiError 400 si le numéro existe déjà. */
    create: async (request: MaterialRequestDto): Promise<Material> => {
        const dto = await apiRequest<MaterialDto>('/materials', {
            method: 'POST',
            body: JSON.stringify(request),
        });
        return materialFromDto(dto);
    },

    /** Relocalise le matériel dans une salle (MANAGER, ADMIN). Refusé si le matériel est archivé (RG-06). */
    assignRoom: async (id: number, roomId: number | null): Promise<Material> => {
        const body: MaterialAssignRequestDto = {roomId};
        const dto = await apiRequest<MaterialDto>(`/materials/${id}/room`, {
            method: 'PATCH',
            body: JSON.stringify(body),
        });
        return materialFromDto(dto);
    },
};
