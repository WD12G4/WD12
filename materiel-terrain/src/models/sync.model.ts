/**
 * Actions faites hors réseau, gardées sur le téléphone puis rejouées vers l'API dans l'ordre.
 * Utilisé par : features/inventory
 */
import type {MaterialDto, MaterialRequestDto} from './material.dto.ts';

//----------------- VALEURS ------------------------------
/** Libellés des actions en attente. */
const pendingActionLabels = {
    relocate: 'Relocalisation',
    create: 'Prise en compte',
    sight: 'Constat',
    'clear-room': 'Nouvelle reconnaissance',
} as const;

//----------------- TYPES --------------------------------
/**
 * Nature d'une action : PATCH /materials/{id}/room, POST /materials,
 * POST /sightings (scan dans une salle) ou DELETE /sightings?roomId= (constats de la salle effacés).
 */
type PendingActionKind = keyof typeof pendingActionLabels;

/** Action en attente d'envoi. Enregistrée en JSON : les dates sont des chaînes ISO. */
interface PendingAction {
    id: string;
    kind: PendingActionKind;
    /** Date de l'action sur le terrain (ISO). */
    createdAt: string;
    serialNumber: string;
    /** relocate : matériel visé ; create : identifiant provisoire (négatif) du matériel créé hors ligne ; sinon 0. */
    materialId: number;
    /** Salle cible, ou null (en stock) ; sight, clear-room : salle du constat. */
    roomId: number | null;
    /** create : corps de la requête ; relocate : null. */
    request: MaterialRequestDto | null;
    /**
     * relocate : salle déclarée que l'agent avait sous les yeux quand il a agi (null = en stock).
     * Sert à détecter un conflit au moment de l'envoi. Absente sur les actions d'une version précédente.
     */
    baseRoomId?: number | null;
    /** sight : date du scan (ISO). */
    seenAt?: string;
    /** sight : numéro lu puis corrigé par l'agent, dont le constat est retiré. */
    replaces?: string;
    /** Message de l'API si elle a refusé l'action : elle n'est plus rejouée automatiquement. */
    error: string | null;
    /** État du serveur quand il contredit l'action (modifié entre-temps par quelqu'un d'autre) : l'agent arbitre. */
    conflict: MaterialDto | null;
}

/** Vrai pour une action sur le parc (création, relocalisation), dont l'ordre compte ; faux pour un constat. */
function isMaterialAction(action: PendingAction): boolean {
    return action.kind === 'create' || action.kind === 'relocate';
}

export {pendingActionLabels, isMaterialAction};
export type {PendingActionKind, PendingAction};
