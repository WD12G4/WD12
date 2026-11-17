/**
 * Modèle métier du matériel, tel que manipulé par le front.
 * Les dates sont des objets Date (la conversion se fait dans material.dto.ts).
 * RG : RG-01, RG-03, RG-04, RG-07, RG-10
 */
import type {Room} from './room.model.ts';

//----------------- VALEURS ------------------------------
/** Longueur maximale du numéro de série, identique au backend (RG-01). */
const serialNumberMaxLength = 100;

//----------------- TYPES --------------------------------
/**
 * Code d'un type de matériel (ex. 'UC'). La liste et les libellés viennent de l'API
 * (enum EMaterialType du backend, GET /material-types/) : voir use-material-types.ts.
 * Un switch (matériel réseau cité par le client, RG-03) est un « Périphérique » : décision d'équipe du 17/11.
 */
type MaterialType = string;

/** Type de matériel proposé par l'API : code et libellé affiché. */
interface MaterialTypeOption {
    code: MaterialType;
    label: string;
}

/** Matériel suivi à l'unité. */
interface Material {
    id: number;
    /** Version pour le verrouillage optimiste, renvoyée par l'API. */
    version: number;
    /** Numéro de série, unique (RG-01). */
    serialNumber: string;
    materialType: MaterialType;
    /** Date de fin de disponibilité. null si aucune (RG-04). */
    endAvailabilityDate: Date | null;
    /** null = matériel en stock (RG-07). */
    room: Room | null;
    /** Vrai si le matériel est archivé (suppression logique, RG-10). */
    archived: boolean;
}

/** Données saisies dans le formulaire de création ou de modification. */
interface MaterialInput {
    serialNumber: string;
    materialType: MaterialType;
    /** Format 'AAAA-MM-JJ', tel que renvoyé par <input type="date">. */
    endAvailabilityDate: string | null;
    roomId: number | null;
    archived: boolean;
}

/** Valeur du filtre « type » : un type précis, ou tous. */
type MaterialTypeFilter = 'all' | MaterialType;

/** Valeur du filtre « emplacement » : tous, affectés à une salle, ou en stock (RG-07). */
type MaterialLocationFilter = 'all' | 'assigned' | 'stock';

/** Critères de la recherche de matériel. */
interface MaterialFilters {
    /** Texte cherché dans le numéro de série, le numéro de salle et le numéro de bâtiment. */
    search: string;
    type: MaterialTypeFilter;
    /** Identifiant du bâtiment, ou 'all'. */
    buildingId: number | 'all';
    /** Identifiant de la salle, ou 'all'. */
    roomId: number | 'all';
    location: MaterialLocationFilter;
    /** Affiche aussi les matériels archivés. */
    showArchived: boolean;
}

export {serialNumberMaxLength};
export type {
    MaterialType, MaterialTypeOption, Material, MaterialInput, MaterialTypeFilter, MaterialLocationFilter, MaterialFilters,
};
