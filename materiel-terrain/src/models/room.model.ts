/**
 * Modèle métier de la salle (lecture seule sur le terrain).
 * RG : RG-13 (une salle appartient à un seul bâtiment, numéro unique dans ce bâtiment)
 */
import type {Building} from './building.model.ts';

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

export type {Room};
