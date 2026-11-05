/**
 * Modèle métier des comptes et des rôles.
 * RG : RG-14 (trois rôles), RG-15 (l'administrateur hérite de tous les droits)
 */

//------------------ VALEURS ------------------------------
/** Rôles acceptés par l'API (ERole côté backend), du moins au plus élevé. */
const roles = ['USER', 'MANAGER', 'ADMIN'] as const;

/** Libellés affichés : les codes techniques ne sont jamais montrés à l'écran. */
const roleLabels = {
    USER: 'Formateur',
    MANAGER: 'Gestionnaire',
    ADMIN: 'Administrateur',
} as const;

/**
 * Rang de chaque rôle. Un rôle possède tous les droits des rangs inférieurs,
 * comme ERole.getAllRoles() côté backend : ADMIN ⊃ MANAGER ⊃ USER (RG-15).
 */
const roleRanks = {
    USER: 0,
    MANAGER: 1,
    ADMIN: 2,
} as const;

//------------------ TYPES --------------------------------
/** Code d'un rôle. */
type Role = (typeof roles)[number];

/** Compte utilisateur. */
interface User {
    id: number;
    /** Version pour le verrouillage optimiste, renvoyée par l'API. */
    version: number;
    /** Identifiant de connexion, unique. */
    username: string;
    role: Role;
    /** Un compte désactivé ne peut pas se connecter (RG-19). */
    enabled: boolean;
}

export {roles, roleLabels, roleRanks};
export type {Role, User};
