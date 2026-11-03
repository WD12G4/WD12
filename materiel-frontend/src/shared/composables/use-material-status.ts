/**
 * Statut affiché d'un matériel, calculé à partir des données de l'API.
 * Utilisé par : shared/components/material-status-badge.vue, features/home (compteurs).
 * RG : RG-07 (affecté / en stock), RG-10 (archivé), RG-24 (bientôt expiré)
 *
 * TODO(question): les statuts « HS » et « À rendre » (RG-08) attendent un champ côté backend.
 */
import type {Material} from '../../models/material.model.ts';
import {isExpiringSoon} from './use-dates.ts';

/** Statut principal d'un matériel. La valeur sert aussi de classe CSS du badge. */
type MaterialStatus = 'archived' | 'assigned' | 'stock';

/** Libellés affichés pour chaque statut. */
export const materialStatusLabels = {
    archived: 'Archivé',
    assigned: 'Affecté',
    stock: 'En stock',
} as const;

/**
 * Statut principal : archivé (RG-10), sinon affecté à une salle, sinon en stock (RG-07).
 * @param material matériel à évaluer
 */
export function materialStatus(material: Material): MaterialStatus {
    if (material.archived) return 'archived';
    return material.room ? 'assigned' : 'stock';
}

/**
 * Vrai si le matériel, non archivé, arrive bientôt en fin de disponibilité (RG-24).
 * @param material matériel à évaluer
 */
export function isMaterialExpiring(material: Material): boolean {
    return !material.archived && isExpiringSoon(material.endAvailabilityDate);
}

export type {MaterialStatus};
