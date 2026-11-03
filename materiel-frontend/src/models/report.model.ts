/**
 * Modèle d'une remontée de problème (signalement d'un matériel défectueux).
 * MAQUETTE : aucune API backend n'existe encore pour les remontées.
 * RG : RG-21 (contenu), RG-22 (document), RG-23 (notification du gestionnaire)
 */

//----------------- VALEURS ------------------------------
/**
 * Types de problème proposés dans la liste déroulante.
 * TODO(question): liste non fournie par le client (« type de problème » en liste déroulante,
 * entretien 1, 8:38). Ces valeurs sont une proposition à faire valider.
 */
const problemTypes = ['OUT_OF_ORDER', 'MALFUNCTION', 'MISSING', 'OTHER'] as const;

/** Libellés affichés. */
const problemTypeLabels = {
    OUT_OF_ORDER: 'Hors service',
    MALFUNCTION: 'Fonctionne mal',
    MISSING: 'Manquant',
    OTHER: 'Autre',
} as const;

/** Longueur maximale de la description libre (proposition, aucune contrainte backend connue). */
const reportDescriptionMaxLength = 500;

//----------------- TYPES --------------------------------
/** Code d'un type de problème. */
type ProblemType = (typeof problemTypes)[number];

/** Remontée enregistrée. Les libellés sont figés au moment de la remontée, comme sur un document. */
interface Report {
    id: number;
    createdAt: Date;
    /** Identifiant de connexion de l'auteur. */
    author: string;
    roomId: number;
    /** Ex. « Bât. 12 – Salle 104 ». */
    roomLabel: string;
    /** Numéro de série du matériel concerné, ou null si le problème concerne la salle. */
    materialSerialNumber: string | null;
    problemType: ProblemType;
    description: string;
    /** Faux tant que le gestionnaire ne l'a pas consultée (RG-23). */
    read: boolean;
}

/** Saisie du formulaire de remontée (RG-21). */
interface ReportInput {
    roomId: number | null;
    materialId: number | null;
    problemType: ProblemType;
    description: string;
}

export {problemTypes, problemTypeLabels, reportDescriptionMaxLength};
export type {ProblemType, Report, ReportInput};
