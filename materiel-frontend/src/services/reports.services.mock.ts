// MAQUETTE : aucun endpoint backend. À remplacer par reports.services.ts quand l'API existera.
/**
 * Service simulé des remontées : même forme qu'un vrai service, données en mémoire.
 * Les salles, le matériel et l'auteur viennent, eux, de la vraie API.
 * Les remontées sont perdues au rechargement de la page (aucune persistance).
 * Utilisé par : features/reports
 * RG : RG-21, RG-22, RG-23
 */
import type {Report, ReportInput} from '../models/report.model.ts';
import {authServices} from './auth.services.ts';
import {materialsServices} from './materials.services.ts';
import {roomsServices} from './rooms.services.ts';
import {ApiError} from './api-error.ts';
import {roomLabel} from '../shared/composables/use-room-label.ts';

/** Latence simulée, pour que les états de chargement soient visibles. */
const MOCK_LATENCY_MS = 300;

//------------------ DATAS -----------------------------------
const reports: Report[] = [];
let nextId = 1;

//------------------ FUNCTIONS -------------------------------
/** Attend la latence simulée. */
function delay(): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, MOCK_LATENCY_MS));
}

/** Copie une remontée, pour que l'appelant ne modifie pas les données « serveur » par erreur. */
function copy(report: Report): Report {
    return {...report, createdAt: new Date(report.createdAt)};
}

/** Construit la remontée à enregistrer à partir de la saisie et des données de l'API. */
async function buildReport(input: ReportInput): Promise<Report> {
    if (input.roomId === null) throw new ApiError(400, 'La salle doit être renseignée.');
    const [author, room, material] = await Promise.all([
        authServices.me(),
        roomsServices.findById(input.roomId),
        input.materialId === null ? Promise.resolve(null) : materialsServices.findById(input.materialId),
    ]);
    return {
        id: nextId++,
        createdAt: new Date(),
        author: author.username,
        roomId: room.id,
        roomLabel: roomLabel(room),
        materialSerialNumber: material?.serialNumber ?? null,
        problemType: input.problemType,
        description: input.description.trim(),
        read: false,
    };
}

export const reportsServices = {

    /** Liste les remontées, de la plus récente à la plus ancienne. */
    findAll: async (): Promise<Report[]> => {
        await delay();
        return [...reports].reverse().map(copy);
    },

    /** Récupère une remontée. Lève ApiError 404 si elle n'existe pas. */
    findById: async (id: number): Promise<Report> => {
        await delay();
        const report = reports.find(item => item.id === id);
        if (!report) throw new ApiError(404, 'Remontée introuvable.');
        return copy(report);
    },

    /**
     * Enregistre une remontée (RG-21). Le document d'état des lieux est disponible dès sa création (RG-22).
     * TODO(question): le PDF doit-il être généré par le backend (recommandé : « créé, stocké, récupérable ») ?
     */
    create: async (input: ReportInput): Promise<Report> => {
        await delay();
        const report = await buildReport(input);
        reports.push(report);
        return copy(report);
    },

    /** Marque une remontée comme lue par le gestionnaire (RG-23). */
    markAsRead: async (id: number): Promise<void> => {
        await delay();
        const report = reports.find(item => item.id === id);
        if (report) report.read = true;
    },

    /** Nombre de remontées non lues, pour la pop-up de connexion (RG-23). */
    countUnread: async (): Promise<number> => {
        await delay();
        return reports.filter(report => !report.read).length;
    },
};
