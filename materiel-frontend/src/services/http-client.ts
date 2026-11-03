/**
 * Client HTTP de l'application : SEUL fichier qui appelle fetch().
 * Ajoute le jeton, rafraîchit la session une fois sur une 401, et convertit les erreurs en ApiError.
 * Utilisé par : tous les fichiers *.services.ts
 *
 * Pour éviter un import circulaire (use-auth → auth.services → http-client → use-auth),
 * ce fichier n'importe pas use-auth : c'est use-auth qui lui fournit ses accesseurs
 * via configureHttpClient().
 */
import {ApiError, NETWORK_ERROR_STATUS} from './api-error.ts';
import {parseBody, toApiError} from './http-response.ts';

/** Préfixe de toutes les routes du backend. */
const API_PREFIX = '/api/v1';

/** Accesseurs fournis par use-auth.ts. */
interface SessionHooks {
    /** Renvoie le jeton d'accès en mémoire, ou null. */
    getToken: () => string | null;
    /** Rafraîchit la session. Renvoie vrai si un nouveau jeton a été obtenu. */
    refresh: () => Promise<boolean>;
    /** Appelé quand la session est définitivement expirée. */
    onSessionExpired: () => void;
}

//------------------ DATAS -----------------------------------
let hooks: SessionHooks = {
    getToken: () => null,
    refresh: async () => false,
    onSessionExpired: () => undefined,
};

//------------------ FUNCTIONS -------------------------------
/**
 * Branche le client HTTP sur la session. Appelé une seule fois par use-auth.ts.
 * @param sessionHooks accesseurs de la session
 */
export function configureHttpClient(sessionHooks: SessionHooks): void {
    hooks = sessionHooks;
}

/** Construit les en-têtes : JSON si un corps est envoyé, jeton si l'utilisateur est connecté. */
function buildHeaders(options: RequestInit): Headers {
    const headers = new Headers(options.headers);
    if (options.body !== undefined) {
        headers.set('Content-Type', 'application/json');
    }
    const token = hooks.getToken();
    if (token) {
        headers.set('Authorization', `Bearer ${token}`);
    }
    return headers;
}

/**
 * Envoie la requête. Une panne réseau devient une ApiError lisible.
 * credentials 'same-origin' : le cookie de rafraîchissement doit partir avec les appels /auth.
 */
async function send(path: string, options: RequestInit): Promise<Response> {
    try {
        return await fetch(`${API_PREFIX}${path}`, {
            ...options,
            credentials: 'same-origin',
            headers: buildHeaders(options),
        });
    } catch {
        throw new ApiError(NETWORK_ERROR_STATUS);
    }
}

/**
 * Rejoue la requête après un rafraîchissement de session.
 * Si le rafraîchissement échoue, la session est terminée et l'utilisateur renvoyé vers la connexion.
 */
async function retryAfterRefresh(path: string, options: RequestInit): Promise<Response> {
    const refreshed = await hooks.refresh();
    if (!refreshed) {
        hooks.onSessionExpired();
        throw new ApiError(401);
    }
    return send(path, options);
}

/**
 * Appelle l'API et renvoie le corps de la réponse.
 * @param path chemin après /api/v1 (ex. '/materials/3')
 * @param options options de fetch (méthode, corps…)
 * @returns le corps JSON typé, ou undefined pour une réponse sans corps
 * @throws ApiError si l'API répond en erreur ou ne répond pas
 */
export async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
    let response = await send(path, options);
    // Le jeton dure 15 minutes : sur une 401, on tente un seul rafraîchissement (section 9.2 du cadre).
    if (response.status === 401 && hooks.getToken() !== null) {
        response = await retryAfterRefresh(path, options);
    }
    if (!response.ok) {
        throw await toApiError(response);
    }
    return parseBody<T>(response);
}

/**
 * Appelle une route d'authentification et renvoie le jeton reçu dans l'en-tête Authorization.
 * Pas de rafraîchissement automatique ici : ces routes servent justement à l'obtenir.
 * @param path chemin après /api/v1 (ex. '/auth/login/')
 * @param options options de fetch
 * @returns le jeton sans le préfixe « Bearer », ou null si l'en-tête est absent
 * @throws ApiError si l'API répond en erreur
 */
export async function apiTokenRequest(path: string, options: RequestInit = {}): Promise<string | null> {
    const response = await send(path, options);
    if (!response.ok) {
        throw await toApiError(response);
    }
    const header = response.headers.get('Authorization');
    return header ? header.replace(/^Bearer\s+/i, '') : null;
}
