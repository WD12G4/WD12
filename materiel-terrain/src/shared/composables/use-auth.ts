/**
 * Session de l'utilisateur : jeton d'accès, profil, connexion, déconnexion et droits.
 * Repris de materiel-frontend, avec une session « hors ligne » pour le terrain :
 * si l'API est injoignable au démarrage, l'agent continue avec son dernier profil connu
 * (consultation du parc en cache, actions mises en attente) jusqu'au retour du réseau.
 * Utilisé par : router/guards.ts, App.vue, features/inventory, features/scan
 * RG : RG-14, RG-15 (héritage des rôles), RG-19 (compte désactivé refusé par le backend)
 *
 * Le jeton reste EN MÉMOIRE : seul le profil (identifiant, rôle) est gardé sur le téléphone.
 */
import {computed, ref} from 'vue';
import {type Role, roleRanks, type User} from '../../models/user.model.ts';
import {authServices} from '../../services/auth.services.ts';
import {configureHttpClient} from '../../services/http-client.ts';
import {isUnreachable} from '../../services/api-error.ts';
import {readStored, removeStored, writeStored} from '../../services/local-store.ts';

/** Clé du dernier profil connu dans le stockage local. */
const PROFILE_KEY = 'profile';

/** Issue d'un rafraîchissement de session. */
type RefreshResult = 'ok' | 'expired' | 'unreachable';

//------------------ DATAS -----------------------------------
const accessToken = ref<string | null>(null);
const currentUser = ref<User | null>(null);
/** Vrai quand l'agent travaille sur son dernier profil connu, sans jeton (API injoignable). */
const offlineSession = ref(false);
let restoring: Promise<void> | null = null;
let refreshing: Promise<RefreshResult> | null = null;
let sessionExpiredHandler: () => void = () => undefined;

//------------------ COMPUTED --------------------------------
const isAuthenticated = computed(() => currentUser.value !== null);

//------------------ FUNCTIONS -------------------------------
/** Vide la session locale (le cookie, lui, est effacé par le backend). */
function clearSession(): void {
    accessToken.value = null;
    currentUser.value = null;
    offlineSession.value = false;
    removeStored(PROFILE_KEY);
}

/** Enregistre le profil chargé depuis l'API : la session est en ligne. */
function setOnlineUser(user: User): void {
    currentUser.value = user;
    offlineSession.value = false;
    writeStored(PROFILE_KEY, user);
}

/**
 * Rafraîchit le jeton grâce au cookie HttpOnly. Un seul appel à la fois, partagé par tous les demandeurs :
 * deux appels simultanés avec le même cookie seraient vus par le backend comme une réutilisation
 * du jeton (RefreshTokenServiceImpl.rotate), qui révoque alors toute la session.
 * @returns 'ok', 'expired' (cookie absent, expiré ou révoqué) ou 'unreachable' (pas de réseau)
 */
function refreshAccessToken(): Promise<RefreshResult> {
    refreshing ??= (async (): Promise<RefreshResult> => {
        try {
            accessToken.value = await authServices.refresh();
            return accessToken.value === null ? 'expired' : 'ok';
        } catch (error) {
            accessToken.value = null;
            return isUnreachable(error) ? 'unreachable' : 'expired';
        } finally {
            refreshing = null;
        }
    })();
    return refreshing;
}

/** Rafraîchit le jeton puis recharge le profil. */
async function restoreOnline(): Promise<RefreshResult> {
    const result = await refreshAccessToken();
    if (result !== 'ok') return result;
    try {
        setOnlineUser(await authServices.me());
        return 'ok';
    } catch (error) {
        return isUnreachable(error) ? 'unreachable' : 'expired';
    }
}

/** Rafraîchissement du seul jeton, demandé par le client HTTP sur une 401. */
async function refreshToken(): Promise<boolean> {
    return (await refreshAccessToken()) === 'ok';
}

/**
 * Connecte l'utilisateur, puis charge son profil.
 * @throws ApiError 401 si les identifiants sont faux ou le compte désactivé
 */
async function login(username: string, password: string): Promise<void> {
    accessToken.value = await authServices.login({username: username.trim(), password});
    setOnlineUser(await authServices.me());
}

/** Déconnecte l'utilisateur. La session locale est vidée même si l'appel échoue (hors réseau). */
async function logout(): Promise<void> {
    try {
        await authServices.logout();
    } catch {
        // Hors réseau : le cookie expirera seul.
    } finally {
        clearSession();
    }
}

/**
 * Restaure la session au démarrage. Sans réseau, reprend le dernier profil connu (session hors ligne).
 * Appelée une seule fois par la garde de route.
 */
function restoreSession(): Promise<void> {
    restoring ??= (async () => {
        const result = await restoreOnline();
        const profile = readStored<User | null>(PROFILE_KEY, null);
        if (result === 'unreachable' && profile) {
            currentUser.value = profile;
            offlineSession.value = true;
        } else if (result === 'expired') {
            clearSession();
        }
    })();
    return restoring;
}

/**
 * Au retour du réseau, remet une session hors ligne en ligne (nouveau jeton via le cookie).
 * Si le cookie a expiré, la session se termine et l'agent est renvoyé vers la connexion
 * (les actions en attente sont conservées et partiront après sa reconnexion).
 * @returns 'ok' (jeton valide), 'expired' (reconnexion nécessaire) ou 'unreachable' (pas de réseau)
 */
async function reconnect(): Promise<RefreshResult> {
    if (accessToken.value !== null) return 'ok';
    if (currentUser.value === null) return 'expired';
    const result = await restoreOnline();
    if (result === 'expired') {
        clearSession();
        sessionExpiredHandler();
    }
    return result;
}

/**
 * Indique si l'utilisateur connecté possède un rôle, en tenant compte de l'héritage (RG-15).
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
    return {
        currentUser, isAuthenticated, offlineSession,
        login, logout, restoreSession, reconnect, hasRole, onSessionExpired,
    };
}

export type {RefreshResult};
