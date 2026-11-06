/**
 * Modèle métier du matériel, tel que manipulé par la PWA terrain.
 * Les dates sont des objets Date (la conversion se fait dans material.dto.ts).
 * RG : RG-01, RG-03, RG-04, RG-07, RG-10
 */
import type {Room} from './room.model.ts';

//----------------- VALEURS ------------------------------
/** Types de matériel acceptés par l'API (EMaterialType côté backend). */
const materialTypes = ['UC', 'SCREEN', 'PROJECTOR', 'PERIPHERAL'] as const;

/** Libellés affichés. L'API ne les fournit pas : ils sont définis ici. */
const materialTypeLabels = {
    UC: 'Unité centrale',
    SCREEN: 'Écran',
    PROJECTOR: 'Projecteur',
    PERIPHERAL: 'Périphérique',
} as const;

/** Longueur maximale du numéro de série, identique au backend (RG-01). */
const serialNumberMaxLength = 100;

//----------------- TYPES --------------------------------
/** Code d'un type de matériel. */
type MaterialType = (typeof materialTypes)[number];

/** Matériel suivi à l'unité. */
interface Material {
    /** Identifiant de l'API. Négatif = pris en compte hors ligne, pas encore enregistré (voir isProvisional). */
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

/** Données saisies pour prendre en compte un matériel non recensé. */
interface MaterialInput {
    serialNumber: string;
    materialType: MaterialType;
    /** Format 'AAAA-MM-JJ', tel que renvoyé par <input type="date">. */
    endAvailabilityDate: string;
    /** Salle où le matériel a été trouvé, ou null (en stock). */
    roomId: number | null;
}

//----------------- FONCTIONS ----------------------------
/**
 * Indique si le matériel a été pris en compte hors ligne et attend encore sa création par l'API.
 * @param material matériel à tester
 */
function isProvisional(material: Material): boolean {
    return material.id < 0;
}

export {materialTypes, materialTypeLabels, serialNumberMaxLength, isProvisional};
export type {MaterialType, Material, MaterialInput};
