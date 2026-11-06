/**
 * Regroupement des salles par bâtiment, triés par numéro.
 * Utilisé par : room-picker-view.vue (choix de la position), material-relocation.vue (changement de salle)
 */
import type {Room} from '../../../models/room.model.ts';

/** Salles d'un même bâtiment. */
interface BuildingGroup {
    building: number;
    rooms: Room[];
}

/**
 * Regroupe les salles par numéro de bâtiment.
 * @param rooms salles à regrouper
 */
export function groupRoomsByBuilding(rooms: Room[]): BuildingGroup[] {
    const byBuilding = new Map<number, Room[]>();
    for (const room of rooms) {
        byBuilding.set(room.building.number, [...(byBuilding.get(room.building.number) ?? []), room]);
    }
    return [...byBuilding.entries()]
        .sort(([a], [b]) => a - b)
        .map(([building, list]) => ({building, rooms: [...list].sort((a, b) => a.number - b.number)}));
}

export type {BuildingGroup};
