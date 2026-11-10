/**
 * Libellés lisibles des emplacements (« Bât. 12 – Salle 104 »).
 * Utilisé par : room-select.vue, features/materials, features/rooms, features/reports, features/supervision.
 */
import type {Room} from '../../models/room.model.ts';

/**
 * Libellé d'une salle avec son bâtiment.
 * @param room salle à décrire
 */
export function roomLabel(room: Room): string {
    return `Bât. ${room.building.number} – Salle ${room.number}`;
}

/**
 * Libellé de l'emplacement d'un matériel : sa salle, ou « En stock » s'il n'en a pas (RG-07).
 * @param room salle du matériel, ou null
 */
export function locationLabel(room: Room | null): string {
    return room ? roomLabel(room) : 'En stock';
}

/**
 * Ordre d'affichage des salles : par numéro de bâtiment, puis par numéro de salle (pour sort()).
 * @param a première salle
 * @param b seconde salle
 */
export function byBuildingThenNumber(a: Room, b: Room): number {
    return a.building.number - b.building.number || a.number - b.number;
}
