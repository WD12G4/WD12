/**
 * Compteurs du tableau de bord (le client préfère les chiffres aux graphiques, entretien 1, 13:37).
 * Utilisé par : home-view.vue
 * RG : RG-07 (affecté / en stock), RG-10 (archivés exclus), RG-24 (alerte d'expiration groupée)
 */
import {computed, type Ref} from 'vue';
import type {Material} from '../../../models/material.model.ts';
import {isMaterialExpiring, materialStatus} from '../../../shared/composables/use-material-status.ts';

/** Compteurs affichés. */
interface DashboardStats {
    /** Matériel non archivé. */
    active: number;
    assigned: number;
    stock: number;
    /** Matériel non archivé dont la fin de disponibilité approche (RG-24). */
    expiring: number;
}

/**
 * Calcule les compteurs à partir de la liste complète du matériel.
 * @param materials matériel chargé par la vue
 */
export function useDashboardStats(materials: Ref<Material[]>) {
    const stats = computed<DashboardStats>(() => {
        const statuses = materials.value.map(materialStatus);
        return {
            active: statuses.filter(status => status !== 'archived').length,
            assigned: statuses.filter(status => status === 'assigned').length,
            stock: statuses.filter(status => status === 'stock').length,
            expiring: materials.value.filter(isMaterialExpiring).length,
        };
    });
    return {stats};
}

export type {DashboardStats};
