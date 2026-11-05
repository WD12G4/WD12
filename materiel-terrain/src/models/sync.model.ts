/**
 * Actions faites hors réseau, gardées sur le téléphone puis rejouées vers l'API dans l'ordre.
 * Utilisé par : features/inventory
 */
import type {MaterialRequestDto} from './material.dto.ts';

//----------------- VALEURS ------------------------------
/** Libellés des actions en attente. */
const pendingActionLabels = {
    relocate: 'Relocalisation',
    create: 'Prise en compte',
} as const;

//----------------- TYPES --------------------------------
/** Nature d'une action : PATCH /materials/{id}/room ou POST /materials. */
type PendingActionKind = keyof typeof pendingActionLabels;

/** Action en attente d'envoi. Enregistrée en JSON : les dates sont des chaînes ISO. */
interface PendingAction {
    id: string;
    kind: PendingActionKind;
    /** Date de l'action sur le terrain (ISO). */
    createdAt: string;
    serialNumber: string;
    /** relocate : matériel visé ; create : identifiant provisoire (négatif) du matériel créé hors ligne. */
    materialId: number;
    /** Salle cible, ou null (en stock). */
    roomId: number | null;
    /** create : corps de la requête ; relocate : null. */
    request: MaterialRequestDto | null;
    /** Message de l'API si elle a refusé l'action : elle n'est plus rejouée automatiquement. */
    error: string | null;
}

export {pendingActionLabels};
export type {PendingActionKind, PendingAction};
