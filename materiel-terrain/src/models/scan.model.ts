/**
 * Modèle d'une lecture terrain : ce qui a été lu (code ou saisie) et le verdict par rapport au parc connu.
 * Utilisé par : features/scan
 */
import type {Material, MaterialType} from './material.model.ts';

//----------------- VALEURS ------------------------------
/** Libellés des verdicts, affichés dans la carte de résultat et l'historique. */
const verdictLabels = {
    conform: 'Conforme',
    moved: 'Déplacé',
    stock: 'Déclaré en stock',
    archived: 'Archivé',
    known: 'Identifié',
    unknown: 'Non recensé',
} as const;

//----------------- TYPES --------------------------------
/** Origine de la lecture. */
type ScanSource = 'camera' | 'photo' | 'manual';

/**
 * Verdict d'une lecture :
 * conform = dans la salle courante ; moved = déclaré dans une autre salle ; stock = déclaré sans salle ;
 * archived = archivé (ne peut plus être affecté, RG-06) ; known = identifié mais aucune salle courante choisie ;
 * unknown = absent du parc.
 */
type ScanVerdict = keyof typeof verdictLabels;

/** Données extraites du texte lu. */
interface ScanReading {
    /** Texte brut tel que lu ou saisi. */
    raw: string;
    /** Numéro de série retenu : le candidat le plus probable. */
    serialNumber: string;
    /** Numéros de série possibles, du plus probable au moins probable. */
    candidates: string[];
    /** Adresse MAC probable (12 caractères hexadécimaux), ou null. */
    macAddress: string | null;
    /** Numéro OTAN (NNO) au format 7021-14-602-7780, ou null. */
    nsn: string | null;
    /** Type de matériel déduit du NNO, à confirmer par l'utilisateur, ou null. */
    typeHint: MaterialType | null;
    source: ScanSource;
}

/** Résultat d'une lecture, affiché à l'utilisateur. */
interface ScanOutcome {
    reading: ScanReading;
    verdict: ScanVerdict;
    /** Matériel identifié, ou null s'il est inconnu. */
    material: Material | null;
    at: Date;
}

export {verdictLabels};
export type {ScanSource, ScanVerdict, ScanReading, ScanOutcome};
