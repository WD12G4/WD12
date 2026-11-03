/**
 * Lecture des réponses HTTP : corps JSON et conversion des erreurs en ApiError.
 * Utilisé par : http-client.ts uniquement.
 */
import {ApiError} from './api-error.ts';

/** Convertit une réponse en erreur, en lisant le message { "error": … } du backend s'il existe. */
export async function toApiError(response: Response): Promise<ApiError> {
    try {
        const body = (await response.json()) as { error?: string };
        return new ApiError(response.status, body.error);
    } catch {
        // Corps vide ou non JSON (401 de la connexion, par exemple).
        return new ApiError(response.status);
    }
}

/** Lit le corps JSON, ou renvoie undefined quand il n'y en a pas (204, ou 200 sans corps). */
export async function parseBody<T>(response: Response): Promise<T> {
    const text = await response.text();
    return (text.length > 0 ? JSON.parse(text) : undefined) as T;
}
