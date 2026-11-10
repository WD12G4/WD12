/**
 * Modèle d'une remontée de problème (signalement d'un matériel défectueux).
 * Source : /api/v1/reports (ReportResponse). Le backend enregistre l'auteur et la date ; le
 * gestionnaire marque la remontée comme traitée.
 * RG : RG-21 (contenu), RG-22 (document), RG-23 (notification du gestionnaire)
 */
import type {Material} from './material.model.ts';

//----------------- VALEURS ------------------------------
/**
 * Types de problème proposés dans la liste déroulante.
 * TODO(question): liste non fournie par le client (« type de problème » en liste déroulante,
 * entretien 1, 8:38) et sans champ côté backend : le type est écrit en tête de la description
 * (report.dto.ts). Demander un champ dédié à l'équipe backend.
 */
const problemTypes = ['OUT_OF_ORDER', 'MALFUNCTION', 'MISSING', 'OTHER'] as const;

/** Libellés affichés. */
const problemTypeLabels = {
    OUT_OF_ORDER: 'Hors service',
    MALFUNCTION: 'Fonctionne mal',
    MISSING: 'Manquant',
    OTHER: 'Autre',
} as const;

/** Longueur maximale de la description envoyée (contrainte du backend : 500 caractères, type compris). */
const reportDescriptionMaxLength = 450;

//----------------- TYPES --------------------------------
/** Code d'un type de problème. */
type ProblemType = (typeof problemTypes)[number];

/** Remontée enregistrée. */
interface Report {
    id: number;
    createdAt: Date;
    /** Identifiant de connexion de l'auteur. */
    author: string;
    /** Matériel concerné, avec sa salle au moment de la lecture. */
    material: Material;
    /** Type de problème, ou null si la description n'en indique pas. */
    problemType: ProblemType | null;
    description: string;
    /** Vrai une fois traitée par le gestionnaire (RG-23 : seules les autres sont signalées). */
    handled: boolean;
    handledAt: Date | null;
}

/** Saisie du formulaire de remontée (RG-21). La salle sert à filtrer le matériel proposé. */
interface ReportInput {
    roomId: number | null;
    materialId: number | null;
    problemType: ProblemType;
    description: string;
}

export {problemTypes, problemTypeLabels, reportDescriptionMaxLength};
export type {ProblemType, Report, ReportInput};
