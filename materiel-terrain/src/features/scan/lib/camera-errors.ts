/**
 * Messages affichés quand la caméra ne peut pas être utilisée.
 * Utilisé par : features/scan/composables/use-camera-scanner.ts
 */

/** Le navigateur n'expose pas la caméra : page servie en HTTP ailleurs que sur localhost. */
export const INSECURE_CONTEXT_MESSAGE = "Caméra indisponible : ouvrez l'application en HTTPS (voir README), "
    + 'ou utilisez la photo ou la saisie manuelle.';

/**
 * Traduit une erreur de getUserMedia en message pour l'agent.
 * @param error erreur levée au démarrage de la caméra
 */
export function cameraErrorMessage(error: unknown): string {
    const name = error instanceof DOMException ? error.name : '';
    if (name === 'NotAllowedError') {
        return "Accès à la caméra refusé. Autorisez-le dans Réglages › Safari › Caméra, ou utilisez la photo.";
    }
    if (name === 'NotFoundError' || name === 'OverconstrainedError') {
        return 'Aucune caméra disponible sur cet appareil. Utilisez la saisie manuelle.';
    }
    return 'Impossible de démarrer la caméra. Utilisez la photo ou la saisie manuelle.';
}
