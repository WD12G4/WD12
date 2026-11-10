/**
 * Accès à la ressource /api/v1/reports (remontées de problème).
 * Toutes les méthodes lèvent ApiError si l'API répond en erreur.
 * RG : RG-21 (création), RG-22 (document), RG-23 (remontées à traiter, gestionnaire)
 */
import type {Report, ReportInput} from '../models/report.model.ts';
import {type ReportDto, reportFromDto, reportToRequest} from '../models/report.dto.ts';
import {type PageDto, pageContent} from '../models/page.dto.ts';
import {ApiError} from './api-error.ts';
import {apiRequest} from './http-client.ts';

export const reportsServices = {
    /**
     * Liste les remontées, de la plus récente à la plus ancienne.
     * TODO(perf): une seule page large ; à paginer au-delà de 1000 remontées.
     */
    findAll: async (): Promise<Report[]> => {
        const page = await apiRequest<PageDto<ReportDto>>('/reports?size=1000&sort=reportDateTime,desc');
        return pageContent(page).map(reportFromDto);
    },

    /** Récupère une remontée. Lève ApiError 404 si elle n'existe pas. */
    findById: async (id: number): Promise<Report> => reportFromDto(await apiRequest<ReportDto>(`/reports/${id}`)),

    /** Enregistre une remontée (RG-21) ; l'auteur et la date sont fixés par le backend. */
    create: async (input: ReportInput): Promise<Report> => {
        if (input.materialId === null) throw new ApiError(400, 'Le matériel doit être renseigné.');
        const dto = await apiRequest<ReportDto>('/reports', {
            method: 'POST',
            body: JSON.stringify(reportToRequest(input, input.materialId)),
        });
        return reportFromDto(dto);
    },

    /** Marque une remontée comme traitée, ou la rouvre (gestionnaire et administrateur, RG-23). */
    setHandled: async (id: number, handled: boolean): Promise<Report> => {
        const dto = await apiRequest<ReportDto>(`/reports/${id}/handle?handled=${handled}`, {method: 'PATCH'});
        return reportFromDto(dto);
    },

    /** Nombre de remontées à traiter, pour la pop-up de connexion du gestionnaire (RG-23). */
    countUnhandled: async (): Promise<number> => {
        const page = await apiRequest<PageDto<ReportDto>>('/reports?handled=false&size=1');
        return page.totalElements;
    },
};
