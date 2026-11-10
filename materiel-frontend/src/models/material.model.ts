/**
 * Modèle métier du matériel, tel que manipulé par le front.
 * Les dates sont des objets Date (la conversion se fait dans material.dto.ts).
 * RG : RG-01, RG-03, RG-04, RG-07, RG-10
 */
import type {Room} from './room.model.ts';

//----------------- VALEURS ------------------------------
/**
 * Types de matériel acceptés par l'API (EMaterialType côté backend).
 * TODO(question): le client cite des switches (RG-03). Type à part ou « Périphérique » ?
 * Ne pas ajouter de valeur ici tant que le backend ne l'accepte pas.
 */
const materialTypes = ['UC', 'SCREEN', 'PROJECTOR', 'PERIPHERAL'] as const;

/** Libellés affichés. L'API ne les fournit plus : ils sont définis ici. */
const materialTypeLabels = {
    UC: 'Unité centrale',
    SCREEN: 'Écran',
    PROJECTOR: 'Projecteur',
    PERIPHERAL: 'Périphérique',
} as const;

/** Libellés courts, pour les boutons segmentés et les pastilles où la place manque. */
const materialTypeShortLabels = {
    UC: 'UC',
    SCREEN: 'Écran',
    PROJECTOR: 'Projecteur',
    PERIPHERAL: 'Périph.',
} as const;

/** Longueur maximale du numéro de série, identique au backend (RG-01). */
const serialNumberMaxLength = 100;

//----------------- TYPES --------------------------------
/** Code d'un type de matériel. */
type MaterialType = (typeof materialTypes)[number];

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

export {materialTypes, materialTypeLabels, materialTypeShortLabels, serialNumberMaxLength};
export type {
    MaterialType, Material, MaterialInput, MaterialTypeFilter, MaterialLocationFilter, MaterialFilters,
};
