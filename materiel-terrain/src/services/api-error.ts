/**
 * Erreur levée par le client HTTP quand l'API répond en erreur ou ne répond pas.
 * Utilisé par : services/http-client.ts, shared/composables/use-async-state.ts
 */

/** Code utilisé quand le serveur n'a pas répondu du tout (réseau, serveur arrêté). */
export const NETWORK_ERROR_STATUS = 0;

/** Messages affichés pour les codes dont le message du backend n'est pas parlant. */
const statusMessages: Readonly<Record<number, string>> = {
    [NETWORK_ERROR_STATUS]: 'Le serveur ne répond pas. Réessayez plus tard.',
    401: 'Votre session a expiré. Reconnectez-vous.',
    403: "Vous n'avez pas les droits pour cette action.",
    409: 'La donnée a été modifiée entre-temps. Rechargez la page.',
};

/** Message utilisé quand aucun message plus précis n'est disponible. */
const defaultMessage = 'Une erreur inattendue est survenue. Réessayez plus tard.';

/**
 * Erreur d'appel à l'API, avec le code HTTP.
 * Le message est toujours affichable tel quel à l'utilisateur.
 */
export class ApiError extends Error {
    /** Code HTTP de la réponse, ou NETWORK_ERROR_STATUS si le serveur n'a pas répondu. */
    readonly status: number;

    /**
     * @param status code HTTP
     * @param serverMessage message renvoyé par le backend dans { "error": … }, s'il existe
     */
    constructor(status: number, serverMessage?: string) {
        super(ApiError.pickMessage(status, serverMessage));
        this.name = 'ApiError';
        this.status = status;
    }

    /**
     * Choisit le message à afficher.
     * Pour 400 et 404, le message du backend est déjà en français et précis : on le garde.
     * Pour les autres codes, un message fixe est plus clair (le backend renvoie « Forbidden »…).
     */
    private static pickMessage(status: number, serverMessage?: string): string {
        if ((status === 400 || status === 404) && serverMessage) {
            return serverMessage;
        }
        return statusMessages[status] ?? defaultMessage;
    }
}

/**
 * Indique si l'API n'a pas pu être jointe (réseau coupé, délai dépassé, backend arrêté derrière le proxy).
 * Dans ce cas l'action terrain est gardée et rejouée plus tard, au lieu d'être signalée comme refusée.
 * @param error erreur attrapée
 */
export function isUnreachable(error: unknown): boolean {
    return error instanceof ApiError && error.status === NETWORK_ERROR_STATUS;
}
