/**
 * Modèle métier du bâtiment.
 * RG : RG-12 (numéro unique entre 0 et 999)
 */

//----------------- VALEURS ------------------------------
/** Bornes du numéro de bâtiment, identiques à la validation du backend (RG-12). */
const buildingNumberRange = {min: 0, max: 999} as const;

//----------------- TYPES --------------------------------
/** Bâtiment de l'école. */
interface Building {
    id: number;
    /** Version pour le verrouillage optimiste, renvoyée par l'API. */
    version: number;
    /** Numéro du bâtiment, unique (RG-12). */
    number: number;
}

/** Données saisies dans le formulaire de bâtiment. */
interface BuildingInput {
    number: number | null;
}

export {buildingNumberRange};
export type {Building, BuildingInput};
