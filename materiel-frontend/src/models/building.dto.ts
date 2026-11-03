/**
 * Forme JSON échangée avec /api/v1/buildings et conversions vers le modèle.
 * Source : BuildingResponse et BuildingRequest (backend, package dtos.building).
 */
import type {Building, BuildingInput} from './building.model.ts';

/** Réponse de l'API pour un bâtiment. */
export interface BuildingDto {
    id: number;
    version: number;
    number: number;
}

/** Corps attendu par POST et PUT /api/v1/buildings. */
export interface BuildingRequestDto {
    number: number | null;
}

/**
 * Convertit la réponse de l'API en modèle front.
 * @param dto bâtiment reçu de l'API
 * @returns le bâtiment (la forme est identique, la copie évite de partager l'objet reçu)
 */
export function buildingFromDto(dto: BuildingDto): Building {
    return {...dto};
}

/**
 * Convertit la saisie du formulaire en corps de requête.
 * @param input saisie du formulaire
 */
export function buildingToRequest(input: BuildingInput): BuildingRequestDto {
    return {number: input.number};
}
