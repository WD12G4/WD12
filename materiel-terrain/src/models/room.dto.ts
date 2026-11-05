/**
 * Forme JSON échangée avec /api/v1/rooms et conversions vers le modèle.
 * Source : RoomResponse (backend, package dtos.room).
 */
import type {Room} from './room.model.ts';
import {type BuildingDto, buildingFromDto} from './building.dto.ts';

/** Réponse de l'API pour une salle : le bâtiment est imbriqué. */
export interface RoomDto {
    id: number;
    version: number;
    number: number;
    building: BuildingDto;
}

/**
 * Convertit la réponse de l'API en modèle front.
 * @param dto salle reçue de l'API
 */
export function roomFromDto(dto: RoomDto): Room {
    return {...dto, building: buildingFromDto(dto.building)};
}

/**
 * Convertit une salle en forme JSON, pour la garder dans le cache du téléphone.
 * @param room salle à enregistrer
 */
export function roomToDto(room: Room): RoomDto {
    return {...room, building: {...room.building}};
}
