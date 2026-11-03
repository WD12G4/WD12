/**
 * Modèle métier de la salle.
 * RG : RG-13 (une salle appartient à un seul bâtiment, numéro unique dans ce bâtiment)
 */
import type {Building} from './building.model.ts';

//----------------- VALEURS ------------------------------
/** Bornes du numéro de salle, identiques à la validation du backend (RG-13). */
const roomNumberRange = {min: 0, max: 999} as const;

//----------------- TYPES --------------------------------
/** Salle de formation. */
interface Room {
    id: number;
    /** Version pour le verrouillage optimiste, renvoyée par l'API. */
    version: number;
    /** Numéro de la salle, unique dans son bâtiment (RG-13). */
    number: number;
    /** Bâtiment auquel la salle est rattachée. */
    building: Building;
}

/** Données saisies dans le formulaire de salle. */
interface RoomInput {
    buildingId: number | null;
    number: number | null;
}

export {roomNumberRange};
export type {Room, RoomInput};
