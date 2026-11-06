/**
 * Modèle métier du bâtiment (lecture seule sur le terrain).
 * RG : RG-12 (numéro unique entre 0 et 999)
 */

//----------------- TYPES --------------------------------
/** Bâtiment de l'école. */
interface Building {
    id: number;
    /** Version pour le verrouillage optimiste, renvoyée par l'API. */
    version: number;
    /** Numéro du bâtiment, unique (RG-12). */
    number: number;
}

export type {Building};
