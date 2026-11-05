/**
 * Message temporaire affiché en haut de page (ex. « Accès refusé » après une redirection).
 * L'état est partagé au niveau du module : la garde de route écrit, App.vue affiche.
 * Utilisé par : router/guards.ts, App.vue, les vues qui confirment une action.
 */
import {ref} from 'vue';

/** Nature du message, qui détermine sa couleur. */
type FlashKind = 'info' | 'success' | 'error';

/** Message affiché. */
interface FlashMessage {
    kind: FlashKind;
    text: string;
}

/** Durée d'affichage avant disparition automatique. */
const DISPLAY_DURATION_MS = 5000;

//------------------ DATAS -----------------------------------
const message = ref<FlashMessage | null>(null);
let timer: ReturnType<typeof setTimeout> | undefined;

//------------------ FUNCTIONS -------------------------------
/** Efface le message affiché. */
function clear(): void {
    message.value = null;
}

/**
 * Affiche un message, qui disparaît seul au bout de quelques secondes.
 * @param text texte en français
 * @param kind nature du message
 */
function show(text: string, kind: FlashKind = 'info'): void {
    clearTimeout(timer);
    message.value = {kind, text};
    timer = setTimeout(clear, DISPLAY_DURATION_MS);
}

/**
 * Accès au message partagé.
 * @returns le message courant, show() et clear()
 */
export function useFlashMessage() {
    return {message, show, clear};
}

export type {FlashKind, FlashMessage};
