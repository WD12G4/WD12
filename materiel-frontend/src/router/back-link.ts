/**
 * Lien « ← Retour » des fiches et des formulaires : vers la page d'où l'on vient si elle a un libellé,
 * sinon vers la page parente. Il recule dans l'historique quand c'est possible, pour que la liste
 * retrouve ses filtres et son défilement. Sorti de navigation.ts, qui garde la navigation générale.
 * Utilisé par : shared/components/back-link.vue
 */
import { router } from './index.ts';
import { previousRoute } from './navigation.ts';
import type { RouteNames, RouteTarget } from './route-types.ts';

/** Libellé du lien « Retour » selon la page vers laquelle il ramène. */
const backLabels: Partial<Record<RouteNames, string>> = {
    'home': 'Retour au tableau de bord',
    'materials': 'Retour au matériel',
    'material-detail': 'Retour à la fiche du matériel',
    'rooms': 'Retour aux salles',
    'room-detail': 'Retour à la salle',
    'buildings': 'Retour aux bâtiments',
    'users': 'Retour aux comptes',
    'reports': 'Retour aux remontées',
    'supervision': 'Retour à la supervision',
};

/** Lien « Retour » calculé : son libellé, son adresse, et s'il recule dans l'historique. */
export interface BackLinkTarget {
    label: string;
    href: string;
    usesHistory: boolean;
    to: RouteTarget;
}

/**
 * Lien « Retour » de la page courante : vers la page d'où l'on vient si elle a un libellé
 * (les formulaires n'en ont pas), sinon vers la page parente « fallback » (adresse tapée, page rechargée).
 * @param fallback page parente
 */
export function backLinkTarget(fallback: RouteTarget): BackLinkTarget {
    const previous = previousRoute();
    const label = previous ? backLabels[previous.name as RouteNames] : undefined;
    if (previous && label && previous.fullPath !== router.currentRoute.value.fullPath) {
        return {label, href: previous.href, usesHistory: true, to: fallback};
    }
    return {label: backLabels[fallback.name] ?? 'Retour', href: router.resolve(fallback).href, usesHistory: false, to: fallback};
}

/**
 * Suit un lien « Retour » : recule dans l'historique (filtres et transitions gardés) ou va à la page parente.
 * @param back lien calculé par backLinkTarget
 */
export function followBackLink(back: BackLinkTarget) {
    if (back.usesHistory) return router.back();
    return router.push(back.to);
}
