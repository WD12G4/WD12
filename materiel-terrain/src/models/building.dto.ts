/**
 * Forme JSON d'un bâtiment, imbriquée dans les salles renvoyées par l'API.
 * Source : BuildingResponse (backend, package dtos.building).
 */
import type {Building} from './building.model.ts';

/** Réponse de l'API pour un bâtiment. */
export interface BuildingDto {
    id: number;
    version: number;
    number: number;
}

/**
 * Convertit la réponse de l'API en modèle front.
 * @param dto bâtiment reçu de l'API
 * @returns le bâtiment (la forme est identique, la copie évite de partager l'objet reçu)
 */
export function buildingFromDto(dto: BuildingDto): Building {
    return {...dto};
}
