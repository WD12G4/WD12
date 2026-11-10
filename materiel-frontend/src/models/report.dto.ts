/**
 * Forme JSON échangée avec /api/v1/reports et conversions vers le modèle.
 * Source : ReportResponse, ReportCreateRequest (backend, package dtos.report).
 * Le type de problème n'existe pas côté backend : il est écrit en tête de la description
 * (« Hors service — l'écran ne s'allume plus ») et relu à l'affichage.
 */
import {type MaterialDto, materialFromDto} from './material.dto.ts';
import {type ProblemType, problemTypeLabels, problemTypes, type Report, type ReportInput} from './report.model.ts';
import type {Role} from './user.model.ts';
import {parseApiDateTime} from './date-time.dto.ts';

/** Séparateur entre le type de problème et la description libre. */
const TYPE_SEPARATOR = ' — ';

/** Réponse de l'API pour une remontée. */
export interface ReportDto {
    id: number;
    version: number;
    description: string;
    reportDateTime: string;
    user: { id: number; username: string; role: Role } | null;
    material: MaterialDto;
    handleDateTime: string | null;
    handled: boolean;
}

/** Corps attendu par POST /api/v1/reports. */
export interface ReportCreateRequestDto {
    description: string;
    materialId: number;
}

/** Sépare le type de problème (en tête de la description) du texte libre. */
function splitDescription(text: string): { problemType: ProblemType | null; description: string } {
    const problemType = problemTypes.find(type => text === problemTypeLabels[type]
        || text.startsWith(problemTypeLabels[type] + TYPE_SEPARATOR)) ?? null;
    if (!problemType) return {problemType: null, description: text};
    return {problemType, description: text.slice(problemTypeLabels[problemType].length + TYPE_SEPARATOR.length)};
}

/**
 * Convertit la réponse de l'API en modèle front.
 * @param dto remontée reçue de l'API
 */
export function reportFromDto(dto: ReportDto): Report {
    return {
        id: dto.id,
        createdAt: parseApiDateTime(dto.reportDateTime),
        author: dto.user?.username ?? 'Compte supprimé',
        material: materialFromDto(dto.material),
        ...splitDescription(dto.description),
        handled: dto.handled,
        handledAt: dto.handleDateTime ? parseApiDateTime(dto.handleDateTime) : null,
    };
}

/**
 * Convertit la saisie en corps de requête : le type de problème, puis la description s'il y en a une.
 * @param input saisie du formulaire (matériel choisi)
 * @param materialId matériel concerné
 */
export function reportToRequest(input: ReportInput, materialId: number): ReportCreateRequestDto {
    const label = problemTypeLabels[input.problemType];
    const text = input.description.trim();
    return {description: text ? label + TYPE_SEPARATOR + text : label, materialId};
}
