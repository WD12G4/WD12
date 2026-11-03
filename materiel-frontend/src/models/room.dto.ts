/**
 * Forme JSON échangée avec /api/v1/rooms et conversions vers le modèle.
 * Source : RoomResponse et RoomRequest (backend, package dtos.room).
 */
import type {Room, RoomInput} from './room.model.ts';
import {type BuildingDto, buildingFromDto} from './building.dto.ts';

/** Réponse de l'API pour une salle : le bâtiment est imbriqué. */
export interface RoomDto {
    id: number;
    version: number;
    number: number;
    building: BuildingDto;
}

/** Corps attendu par POST et PUT /api/v1/rooms. */
export interface RoomRequestDto {
    buildingId: number | null;
    number: number | null;
}

/**
 * Convertit la réponse de l'API en modèle front.
 * @param dto salle reçue de l'API
 */
export function roomFromDto(dto: RoomDto): Room {
    return {...dto, building: buildingFromDto(dto.building)};
}

/**
 * Convertit la saisie du formulaire en corps de requête.
 * @param input saisie du formulaire
 */
export function roomToRequest(input: RoomInput): RoomRequestDto {
    return {buildingId: input.buildingId, number: input.number};
}
