/**
 * Session de l'utilisateur : qui est connecté, avec quels droits, et les actions de connexion.
 * L'état est déclaré au niveau du module (comme use-project-data.ts du cours) :
 * tous les composants qui appellent useAuth() partagent la même session.
 * La connexion par Keycloak est dans use-keycloak-session.ts ; l'outil de démonstration (changer de rôle
 * sans se reconnecter) dans use-demo-account.ts. Les vues ne passent que par useAuth().
 * Utilisé par : router/guards.ts, App.vue, les vues qui affichent des actions selon le rôle.
 * RG : RG-14, RG-15 (héritage des rôles), RG-19, RG-48 (démonstration), RG-50 (MinDef Connect)
 */
import {computed, ref} from 'vue';
import {type Role, roleRanks, type User} from '../../models/user.model.ts';
import {configureHttpClient} from '../../services/http-client.ts';
import {keycloak} from '../../services/keycloak.ts';
import {useDemoAccount} from './use-demo-account.ts';
import {
    type AuthSession, clearSession, refreshToken, restoreSession, signIn, signOut, syncToken,
} from './use-keycloak-session.ts';

//------------------ DATAS -----------------------------------
const session: AuthSession = {
    accessToken: ref<string | null>(null),
    currentUser: ref<User | null>(null),
    ssoUnavailable: ref(false),
    accountRefused: ref(false),
};
const {demoAccount, requestDemoProfile} = useDemoAccount();
/** Fonction appelée quand la session expire ; fournie par le router pour éviter un import circulaire. */
let sessionExpiredHandler: () => void = () => undefined;

//------------------ COMPUTED --------------------------------
const isAuthenticated = computed(() => session.accessToken.value !== null && session.currentUser.value !== null);

//------------------ FUNCTIONS -------------------------------
/**
 * Indique si l'utilisateur connecté possède un rôle, en tenant compte de l'héritage (RG-15).
 * Exemple : hasRole('MANAGER') est vrai pour un ADMIN.
 * @param role rôle minimal requis
 */
function hasRole(role: Role): boolean {
    const user = session.currentUser.value;
    return user !== null && roleRanks[user.role] >= roleRanks[role];
}

/**
 * Enregistre l'action à mener quand la session expire (redirection vers la connexion).
 * @param handler fonction fournie par router/guards.ts
 */
function onSessionExpired(handler: () => void): void {
    sessionExpiredHandler = handler;
}

/**
 * OUTIL DE DÉMONSTRATION : teste avec le rôle d'un compte de démonstration, sans se déconnecter de Keycloak.
 * @param username compte de démonstration ; celui de la connexion Keycloak revient au compte réel
 * @returns vrai si le profil chargé est bien celui demandé (faux pour un compte non administrateur)
 * @throws ApiError si le profil ne peut pas être chargé (réseau…)
 */
async function actAs(username: string): Promise<boolean> {
    const profile = await requestDemoProfile(username, keycloak.tokenParsed?.preferred_username as string | undefined);
    if (profile) session.currentUser.value = profile;
    return profile !== null;
}

// Le jeton dure 5 minutes (realm Keycloak) : il est renouvelé dès qu'il expire. En cas d'échec,
// la prochaine requête reçoit une 401 et le client HTTP termine la session.
keycloak.onTokenExpired = () => {
    keycloak.updateToken(30).then(() => syncToken(session)).catch(() => undefined);
};

// Branche le client HTTP sur cette session (une seule fois, au chargement du module).
configureHttpClient({
    getToken: () => session.accessToken.value,
    refresh: () => refreshToken(session),
    onSessionExpired: () => {
        clearSession(session);
        sessionExpiredHandler();
    },
    getDemoAccount: () => demoAccount.value,
});

/**
 * Accès à la session partagée.
 * @returns l'état de la session et ses actions
 */
export function useAuth() {
    return {
        currentUser: session.currentUser,
        isAuthenticated,
        ssoUnavailable: session.ssoUnavailable,
        accountRefused: session.accountRefused,
        login: (redirectPath: string, username?: string) => signIn(session, redirectPath, username),
        logout: (redirectPath: string) => signOut(session, redirectPath),
        restoreSession: () => restoreSession(session),
        hasRole,
        onSessionExpired,
        actAs,
    };
}
