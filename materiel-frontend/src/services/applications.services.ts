/**
 * Accès à la ressource /api/v1/applications : catalogue des applications vues sur les postes et liste
 * noire. Le backend ferme automatiquement une application interdite dès qu'un agent la remonte.
 * Toutes les méthodes lèvent ApiError si l'API répond en erreur.
 * Utilisé par : features/supervision
 * RG : RG-27 (liste noire)
 */
import type {Application} from '../models/application.model.ts';
import {type ApplicationDto, applicationFromDto} from '../models/application.dto.ts';
import {type PageDto, pageContent} from '../models/page.dto.ts';
import {apiRequest} from './http-client.ts';

export const applicationsServices = {
    /**
     * Liste le catalogue, par nom.
     * TODO(perf): une seule page large ; à paginer au-delà de 1000 applications.
     */
    findAll: async (): Promise<Application[]> => {
        const page = await apiRequest<PageDto<ApplicationDto>>('/applications?size=1000&sort=name');
        return pageContent(page).map(applicationFromDto);
    },

    /**
     * Interdit ou autorise une application.
     * @param id application
     * @param blacklisted vrai pour l'interdire
     */
    setBlacklisted: async (id: number, blacklisted: boolean): Promise<Application> => {
        const dto = await apiRequest<ApplicationDto>(`/applications/${id}/blacklist`, {
            method: 'PATCH',
            body: JSON.stringify({blacklisted}),
        });
        return applicationFromDto(dto);
    },
};
