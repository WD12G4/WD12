/**
 * Modèle métier des comptes et des rôles.
 * Les mots de passe sont gérés par Keycloak (MinDef Connect) : l'application ne garde que l'identifiant,
 * le rôle et l'activation.
 * RG : RG-14 (trois rôles), RG-15 (l'administrateur hérite de tous les droits), RG-20
 */

//----------------- VALEURS ------------------------------
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

/** Longueur maximale de l'identifiant, identique à la validation du backend (RG-20). */
const usernameMaxLength = 100;

//----------------- TYPES --------------------------------
/** Code d'un rôle. */
type Role = (typeof roles)[number];

/** Compte utilisateur. */
interface User {
    id: number;
    /** Version pour le verrouillage optimiste, renvoyée par l'API. */
    version: number;
    /** Identifiant de connexion, unique (RG-20). */
    username: string;
    role: Role;
    /** Un compte désactivé ne peut pas se connecter (RG-19). */
    enabled: boolean;
}

/** Saisie du formulaire de création de compte (identifiant identique à celui de Keycloak). */
interface UserCreateInput {
    username: string;
    role: Role;
}

/** Saisie du formulaire de modification de compte. */
interface UserUpdateInput {
    role: Role;
    enabled: boolean;
}

export {roles, roleLabels, roleRanks, usernameMaxLength};
export type {Role, User, UserCreateInput, UserUpdateInput};
