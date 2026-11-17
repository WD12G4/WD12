/**
 * Connexion par Keycloak, qui joue MinDef Connect (services/keycloak.ts) : restauration de la session au
 * démarrage, renouvellement du jeton, connexion et déconnexion. L'application ne voit jamais le mot de passe :
 * Keycloak prouve l'identité, le backend applique le rôle du compte (inconnu ou désactivé : refusé).
 * Le jeton reste EN MÉMOIRE (keycloak-js) : jamais dans le stockage du navigateur ni dans un cookie lisible.
 * Utilisé par : use-auth.ts, qui déclare l'état de la session et le passe à ces fonctions.
 * RG : RG-19 (compte désactivé refusé par le backend), RG-50 (MinDef Connect)
 */
import type {Ref} from 'vue';
import type {User} from '../../models/user.model.ts';
import {ApiError} from '../../services/api-error.ts';
import {authServices} from '../../services/auth.services.ts';
import {appUrl, keycloak} from '../../services/keycloak.ts';
import {useDemoAccount} from './use-demo-account.ts';

//------------------ TYPES -----------------------------------
/** État de la session (déclaré dans use-auth.ts), modifié par la connexion. */
export interface AuthSession {
    accessToken: Ref<string | null>;
    currentUser: Ref<User | null>;
    /** Vrai si Keycloak n'a pas répondu au démarrage : la connexion est alors impossible. */
    ssoUnavailable: Ref<boolean>;
    /** Vrai si Keycloak a identifié quelqu'un que l'application refuse (compte inconnu ou désactivé). */
    accountRefused: Ref<boolean>;
}

//------------------ DATAS -----------------------------------
const {demoAccount, saveDemoAccount} = useDemoAccount();
/** Restauration en cours : évite d'initialiser Keycloak plusieurs fois au démarrage. */
let restoring: Promise<void> | null = null;

//------------------ FUNCTIONS -------------------------------
/** Vide la session locale (la session Keycloak, elle, est fermée par signOut). */
export function clearSession(session: AuthSession): void {
    session.accessToken.value = null;
    session.currentUser.value = null;
}

/** Recopie le jeton tenu par keycloak-js (après la connexion ou un renouvellement). */
export function syncToken(session: AuthSession): void {
    session.accessToken.value = keycloak.token ?? null;
}

/**
 * Renouvelle le jeton auprès de Keycloak, appelé par le client HTTP après une 401.
 * @returns vrai si un nouveau jeton a été obtenu
 */
export async function refreshToken(session: AuthSession): Promise<boolean> {
    try {
        await keycloak.updateToken(-1);
        syncToken(session);
        return session.accessToken.value !== null;
    } catch {
        clearSession(session);
        return false;
    }
}

/** Charge le profil du compte identifié par Keycloak ; une 401 signifie que l'application refuse ce compte (RG-19). */
async function loadProfile(session: AuthSession): Promise<void> {
    try {
        session.currentUser.value = await authServices.me();
        session.accountRefused.value = false;
        // Compte de démonstration refusé (le compte Keycloak n'est pas administrateur) : on l'oublie.
        if (demoAccount.value && session.currentUser.value.username !== demoAccount.value) saveDemoAccount(null);
    } catch (error) {
        session.accountRefused.value = error instanceof ApiError && error.status === 401;
        keycloak.clearToken();
        clearSession(session);
    }
}

/**
 * Restaure la session au démarrage : un aller-retour vers Keycloak (sans saisie, sans cadre caché) dit si
 * l'utilisateur est déjà identifié ou revient de la page de connexion, puis le profil est chargé. Appelée une
 * seule fois par la garde de route. Un échec n'est pas une erreur : l'utilisateur est simplement non connecté.
 */
export function restoreSession(session: AuthSession): Promise<void> {
    restoring ??= (async () => {
        const authenticated = await keycloak.init({
            onLoad: 'check-sso',
            checkLoginIframe: false,
            // Adresse de retour sans fragment : un #state=…&code=… resté d'un retour précédent ferait refuser le code.
            redirectUri: appUrl(window.location.pathname + window.location.search),
        }).catch((error: unknown) => {
            // Raison visible dans la console du navigateur (adresse de Keycloak, réseau, configuration…).
            console.error('[Keycloak] Initialisation impossible :', error);
            session.ssoUnavailable.value = true;
            return false;
        });
        if (!authenticated) return;
        syncToken(session);
        await loadProfile(session);
    })();
    return restoring;
}

/**
 * Ouvre la page de connexion de Keycloak (la page de l'application est quittée). Après un compte refusé,
 * Keycloak redemande l'identifiant au lieu de reprendre le même compte.
 * @param redirectPath page de l'application où revenir une fois connecté (ex. '/materiel')
 * @param username identifiant prérempli sur la page de Keycloak (mode démonstration)
 */
export async function signIn(session: AuthSession, redirectPath: string, username?: string): Promise<void> {
    await restoreSession(session);
    saveDemoAccount(null);
    const forceLogin = username !== undefined || session.accountRefused.value;
    await keycloak.login({redirectUri: appUrl(redirectPath), loginHint: username, prompt: forceLogin ? 'login' : undefined});
}

/**
 * Déconnecte l'utilisateur de l'application et de Keycloak (la page de l'application est quittée).
 * @param redirectPath page affichée après la déconnexion (la page de connexion)
 */
export async function signOut(session: AuthSession, redirectPath: string): Promise<void> {
    clearSession(session);
    saveDemoAccount(null);
    await keycloak.logout({redirectUri: appUrl(redirectPath)});
}
