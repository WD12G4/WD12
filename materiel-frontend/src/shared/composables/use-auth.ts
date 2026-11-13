/**
 * Session de l'utilisateur : jeton d'accès, profil, connexion, déconnexion et droits.
 * L'état est déclaré au niveau du module (comme use-project-data.ts du cours) :
 * tous les composants qui appellent useAuth() partagent la même session.
 * Utilisé par : router/guards.ts, App.vue, les vues qui affichent des actions selon le rôle.
 * RG : RG-14, RG-15 (héritage des rôles), RG-19 (compte désactivé refusé par le backend)
 *
 * La connexion passe par Keycloak, qui joue MinDef Connect (services/keycloak.ts) : l'application
 * ne voit jamais le mot de passe. Keycloak prouve l'identité ; le backend retrouve le compte par son
 * identifiant et lui applique son rôle. Un compte inconnu de l'application ou désactivé est refusé.
 * Le jeton reste EN MÉMOIRE (keycloak-js) : jamais dans localStorage, sessionStorage ou un cookie lisible.
 * Après un rechargement de page, la session est retrouvée auprès de Keycloak sans nouvelle saisie
 * (restoreSession : aller-retour rapide vers Keycloak, sans cadre caché, qui fonctionne aussi en HTTP).
 */
import {computed, ref} from 'vue';
import {type Role, roleRanks, type User} from '../../models/user.model.ts';
import {ApiError} from '../../services/api-error.ts';
import {authServices} from '../../services/auth.services.ts';
import {configureHttpClient} from '../../services/http-client.ts';
import {appUrl, keycloak} from '../../services/keycloak.ts';

//------------------ DATAS -----------------------------------
const accessToken = ref<string | null>(null);
const currentUser = ref<User | null>(null);
/** Vrai si Keycloak n'a pas répondu au démarrage : la connexion est alors impossible. */
const ssoUnavailable = ref(false);
/** Vrai si Keycloak a identifié quelqu'un que l'application refuse (compte inconnu ou désactivé). */
const accountRefused = ref(false);
/** Promesse de restauration en cours : évite d'initialiser Keycloak plusieurs fois au démarrage. */
let restoring: Promise<void> | null = null;
/** Fonction appelée quand la session expire ; fournie par le router pour éviter un import circulaire. */
let sessionExpiredHandler: () => void = () => undefined;

//------------------ COMPUTED --------------------------------
const isAuthenticated = computed(() => accessToken.value !== null && currentUser.value !== null);

//------------------ FUNCTIONS -------------------------------
/** Vide la session locale (la session Keycloak, elle, est fermée par logout()). */
function clearSession(): void {
    accessToken.value = null;
    currentUser.value = null;
}

/** Recopie le jeton tenu par keycloak-js (après la connexion ou un renouvellement). */
function syncToken(): void {
    accessToken.value = keycloak.token ?? null;
}

/**
 * Renouvelle le jeton auprès de Keycloak, appelé par le client HTTP après une 401.
 * @returns vrai si un nouveau jeton a été obtenu
 */
async function refreshToken(): Promise<boolean> {
    try {
        await keycloak.updateToken(-1);
        syncToken();
        return accessToken.value !== null;
    } catch {
        clearSession();
        return false;
    }
}

/**
 * Charge le profil du compte identifié par Keycloak.
 * Une 401 du backend signifie que l'application refuse ce compte (inconnu ou désactivé, RG-19).
 */
async function loadProfile(): Promise<void> {
    try {
        currentUser.value = await authServices.me();
        accountRefused.value = false;
    } catch (error) {
        accountRefused.value = error instanceof ApiError && error.status === 401;
        keycloak.clearToken();
        clearSession();
    }
}

/**
 * Restaure la session au démarrage : un aller-retour vers Keycloak (sans saisie) dit si l'utilisateur est
 * déjà identifié (ou revient de la page de connexion), puis le profil est chargé. Appelée une seule fois par
 * la garde de route. Un échec n'est pas une erreur : l'utilisateur est simplement non connecté.
 * Pas de vérification silencieuse dans un cadre caché (silentCheckSsoRedirectUri) : elle exige HTTPS
 * (document.requestStorageAccess) et reste bloquée sur la VM, servie en HTTP.
 */
function restoreSession(): Promise<void> {
    restoring ??= (async () => {
        const authenticated = await keycloak.init({
            onLoad: 'check-sso',
            checkLoginIframe: false,
        }).catch((error: unknown) => {
            // Raison visible dans la console du navigateur (adresse de Keycloak, réseau, configuration…).
            console.error('[Keycloak] Initialisation impossible :', error);
            ssoUnavailable.value = true;
            return false;
        });
        if (!authenticated) return;
        syncToken();
        await loadProfile();
    })();
    return restoring;
}

/**
 * Ouvre la page de connexion de Keycloak (la page de l'application est quittée).
 * Après un compte refusé, Keycloak redemande l'identifiant au lieu de reprendre le même compte.
 * @param redirectPath page de l'application où revenir une fois connecté (ex. '/materiel')
 * @param username identifiant prérempli sur la page de Keycloak (mode démonstration)
 */
async function login(redirectPath: string, username?: string): Promise<void> {
    await restoreSession();
    const forceLogin = username !== undefined || accountRefused.value;
    await keycloak.login({
        redirectUri: appUrl(redirectPath),
        loginHint: username,
        prompt: forceLogin ? 'login' : undefined,
    });
}

/**
 * Déconnecte l'utilisateur de l'application et de Keycloak (la page de l'application est quittée).
 * @param redirectPath page affichée après la déconnexion (la page de connexion)
 */
async function logout(redirectPath: string): Promise<void> {
    clearSession();
    await keycloak.logout({redirectUri: appUrl(redirectPath)});
}

/**
 * Indique si l'utilisateur connecté possède un rôle, en tenant compte de l'héritage (RG-15).
 * Exemple : hasRole('MANAGER') est vrai pour un ADMIN.
 * @param role rôle minimal requis
 */
function hasRole(role: Role): boolean {
    const user = currentUser.value;
    return user !== null && roleRanks[user.role] >= roleRanks[role];
}

/**
 * Enregistre l'action à mener quand la session expire (redirection vers la connexion).
 * @param handler fonction fournie par router/guards.ts
 */
function onSessionExpired(handler: () => void): void {
    sessionExpiredHandler = handler;
}

// Le jeton dure 5 minutes (realm Keycloak) : il est renouvelé dès qu'il expire. En cas d'échec,
// la prochaine requête reçoit une 401 et le client HTTP termine la session.
keycloak.onTokenExpired = () => {
    keycloak.updateToken(30).then(syncToken).catch(() => undefined);
};

// Branche le client HTTP sur cette session (une seule fois, au chargement du module).
configureHttpClient({
    getToken: () => accessToken.value,
    refresh: refreshToken,
    onSessionExpired: () => {
        clearSession();
        sessionExpiredHandler();
    },
});

/**
 * Accès à la session partagée.
 * @returns l'état de la session et ses actions
 */
export function useAuth() {
    return {
        currentUser, isAuthenticated, ssoUnavailable, accountRefused,
        login, logout, restoreSession, hasRole, onSessionExpired,
    };
}
