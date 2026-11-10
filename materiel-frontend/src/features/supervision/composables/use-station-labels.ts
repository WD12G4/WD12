/**
 * Libellés de la supervision : initiales du stagiaire, état du poste, nombre d'applications ouvertes.
 * Utilisé par : station-tile.vue, workstation-panel.vue
 * RG : RG-30 (stagiaire identifié par un libellé, sans compte)
 */
import {type Workstation, workstationStatusLabels} from '../../../models/supervision.model.ts';

/**
 * Initiales d'un stagiaire, pour sa pastille : « Stagiaire 3 » → « S3 », « Léa Martin » → « LM ».
 * @param label libellé du stagiaire
 */
export function traineeInitials(label: string): string {
    const parts = label.trim().split(/\s+/).map(word => (/^\d+$/.test(word) ? word : word.charAt(0)));
    return parts.join('').slice(0, 3).toUpperCase();
}

/**
 * État du poste en clair (« En ligne », « Hors ligne », « Masterisation… »).
 * @param workstation poste
 */
export function statusLabel(workstation: Workstation): string {
    return workstationStatusLabels[workstation.status];
}

/**
 * Nombre d'applications ouvertes en clair ; l'état du poste s'il n'est pas disponible.
 * @param workstation poste
 * @param count nombre d'applications ouvertes
 */
export function openAppsLabel(workstation: Workstation, count: number): string {
    if (workstation.status !== 'online') return statusLabel(workstation);
    if (count === 0) return 'Aucune application';
    return count === 1 ? '1 application' : `${count} applications`;
}
