/**
 * Libellés de la supervision : état du poste, nombre d'applications ouvertes.
 * Utilisé par : station-tile.vue, workstation-panel.vue
 */
import {type Workstation, workstationStatusLabels} from '../../../models/supervision.model.ts';

/**
 * État du poste en clair (« En ligne », « Hors ligne », « Masterisation… »).
 * @param workstation poste
 */
export function statusLabel(workstation: Workstation): string {
    return workstationStatusLabels[workstation.status];
}

/**
 * Nombre d'applications ouvertes en clair ; la raison s'il n'y en a pas à montrer.
 * @param workstation poste
 * @param count nombre d'applications ouvertes
 */
export function openAppsLabel(workstation: Workstation, count: number): string {
    if (workstation.status === 'offline') return 'Agent non connecté (poste éteint ?)';
    if (workstation.status === 'reimaging') return 'Réinstallation en cours';
    if (count === 0) return 'Aucune application';
    return count === 1 ? '1 application' : `${count} applications`;
}
