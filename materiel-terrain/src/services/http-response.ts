/**
 * Lecture des réponses HTTP : corps JSON et conversion des erreurs en ApiError.
 * Utilisé par : http-client.ts uniquement.
 */
import {ApiError, NETWORK_ERROR_STATUS} from './api-error.ts';

/**
 * Convertit une réponse en erreur, en lisant le message { "error": … } du backend s'il existe.
 * Une erreur 5xx sans corps JSON ne vient pas de l'API mais du proxy (Vite, nginx) qui n'a pas pu la joindre :
 * elle est traitée comme une absence de réseau, pour que l'action soit rejouée plus tard.
 */
export async function toApiError(response: Response): Promise<ApiError> {
    try {
        const body = (await response.json()) as { error?: string };
        return new ApiError(response.status, body.error);
    } catch {
        // Corps vide ou non JSON (401 de la connexion, ou proxy sans backend).
        return new ApiError(response.status >= 500 ? NETWORK_ERROR_STATUS : response.status);
    }
}

/** Lit le corps JSON, ou renvoie undefined quand il n'y en a pas (204, ou 200 sans corps). */
export async function parseBody<T>(response: Response): Promise<T> {
    const text = await response.text();
    return (text.length > 0 ? JSON.parse(text) : undefined) as T;
}
