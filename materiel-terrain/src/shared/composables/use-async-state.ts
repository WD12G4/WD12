/**
 * État d'une opération asynchrone : chargement en cours et message d'erreur.
 * Chaque vue crée son propre état (pas de partage entre vues).
 * Utilisé par : toutes les vues qui chargent ou envoient des données.
 */
import {ref, type Ref} from 'vue';
import {ApiError} from '../../services/api-error.ts';

/** Message affiché si l'erreur n'est pas une ApiError (bug, réponse inattendue). */
const unexpectedErrorMessage = 'Une erreur inattendue est survenue. Réessayez plus tard.';

/**
 * Transforme n'importe quelle erreur en message affichable.
 * Les détails techniques (stack trace, objet brut) ne sont jamais montrés.
 * @param error erreur attrapée
 */
export function toErrorMessage(error: unknown): string {
    return error instanceof ApiError ? error.message : unexpectedErrorMessage;
}

/** État d'une opération : chargement, message d'erreur, code HTTP de l'erreur. */
interface AsyncState {
    loading: Ref<boolean>;
    errorMessage: Ref<string | null>;
    /** Code HTTP de la dernière erreur (404 → carte « introuvable »), ou null. */
    errorStatus: Ref<number | null>;
}

/**
 * Exécute une opération en mettant l'état à jour (chargement, puis erreur éventuelle).
 * @param state état à mettre à jour
 * @param operation fonction asynchrone à exécuter
 * @returns vrai si l'opération a réussi
 */
async function runWithState(state: AsyncState, operation: () => Promise<void>): Promise<boolean> {
    state.loading.value = true;
    state.errorMessage.value = null;
    state.errorStatus.value = null;
    try {
        await operation();
        return true;
    } catch (error) {
        state.errorMessage.value = toErrorMessage(error);
        state.errorStatus.value = error instanceof ApiError ? error.status : null;
        return false;
    } finally {
        state.loading.value = false;
    }
}

/**
 * Crée un état de chargement et d'erreur, propre à la vue qui l'appelle.
 * @returns loading, errorMessage, errorStatus et run(operation) qui les met à jour
 */
export function useAsyncState() {
    const state: AsyncState = {loading: ref(false), errorMessage: ref(null), errorStatus: ref(null)};
    const run = (operation: () => Promise<void>): Promise<boolean> => runWithState(state, operation);
    return {...state, run};
}
