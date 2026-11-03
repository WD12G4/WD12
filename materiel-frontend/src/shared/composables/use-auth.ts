/**
 * Session de l'utilisateur : jeton d'accès, profil, connexion, déconnexion et droits.
 * L'état est déclaré au niveau du module (comme use-project-data.ts du cours) :
 * tous les composants qui appellent useAuth() partagent la même session.
 * Utilisé par : router/guards.ts, App.vue, les vues qui affichent des actions selon le rôle.
 * RG : RG-14, RG-15 (héritage des rôles), RG-19 (compte désactivé refusé par le backend)
 *
 * Le jeton reste EN MÉMOIRE : jamais dans localStorage, sessionStorage ou un cookie lisible.
 * Après un rechargement de page, la session est restaurée grâce au cookie HttpOnly (restoreSession).
 */
import {computed, ref} from 'vue';
import {type Role, roleRanks, type User} from '../../models/user.model.ts';
import {authServices} from '../../services/auth.services.ts';
import {configureHttpClient} from '../../services/http-client.ts';

//------------------ DATAS -----------------------------------
const accessToken = ref<string | null>(null);
const currentUser = ref<User | null>(null);
/** Promesse de restauration en cours : évite d'appeler refresh/ plusieurs fois au démarrage. */
let restoring: Promise<void> | null = null;
/** Fonction appelée quand la session expire ; fournie par le router pour éviter un import circulaire. */
let sessionExpiredHandler: () => void = () => undefined;

//------------------ COMPUTED --------------------------------
const isAuthenticated = computed(() => accessToken.value !== null && currentUser.value !== null);

//------------------ FUNCTIONS -------------------------------
/** Vide la session locale (le cookie, lui, est effacé par le backend). */
function clearSession(): void {
    accessToken.value = null;
    currentUser.value = null;
}

/**
 * Rafraîchit le jeton grâce au cookie HttpOnly.
 * @returns vrai si un nouveau jeton a été obtenu
 */
async function refreshToken(): Promise<boolean> {
    try {
        accessToken.value = await authServices.refresh();
        return accessToken.value !== null;
    } catch {
        clearSession();
        return false;
    }
}

/**
 * Connecte l'utilisateur, puis charge son profil.
 * @throws ApiError 401 si les identifiants sont faux ou le compte désactivé
 */
async function login(username: string, password: string): Promise<void> {
    accessToken.value = await authServices.login({username: username.trim(), password});
    currentUser.value = await authServices.me();
}

/** Déconnecte l'utilisateur. La session locale est vidée même si l'appel échoue. */
async function logout(): Promise<void> {
    try {
        await authServices.logout();
    } finally {
        clearSession();
    }
}

/**
 * Restaure la session au démarrage (refresh/ puis me/). Appelée une seule fois par la garde de route.
 * Un échec n'est pas une erreur : l'utilisateur est simplement non connecté.
 */
function restoreSession(): Promise<void> {
    restoring ??= (async () => {
        if (await refreshToken()) {
            currentUser.value = await authServices.me().catch(() => null);
        }
    })();
    return restoring;
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
    return {currentUser, isAuthenticated, login, logout, restoreSession, hasRole, onSessionExpired};
}
