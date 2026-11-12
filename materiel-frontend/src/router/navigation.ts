/**
 * Navigation par programme, typée. Copié du projet du cours (vue1).
 * Toujours passer par navigateTo({ name, params }) : jamais router.push('/chemin').
 * Ajouts pour une navigation cohérente :
 * - replaceWith / returnTo : après un formulaire, il ne reste pas dans l'historique ;
 * - backLinkTarget / followBackLink : le lien « ← Retour » ramène à la page d'où l'on vient
 *   (Vue Router range le chemin de la page précédente dans history.state.back) ;
 * - afterLoginHref / navigateAfterLogin : après la connexion (Keycloak, qui joue MinDef Connect) et le
 *   choix de la salle, retour à la page demandée ;
 * - hrefOf : adresse d'une route, pour les redirections de Keycloak.
 * Utilisé par : les vues, shared/components/back-link.vue
 */
import type {RouteLocationResolved} from 'vue-router';
import { router } from './index.ts';
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
 * Navigue vers une route nommée.
 * @param to cible typée (nom de route et paramètres)
 */
export function navigateTo(to: RouteTarget) {
    return router.push(to);
}

/**
 * Remplace la page courante, qui ne reste pas dans l'historique (ex. formulaire envoyé).
 * @param to cible typée
 */
export function replaceWith(to: RouteTarget) {
    return router.replace(to);
}

/** Page précédente dans l'historique du navigateur, si c'est une page de l'application. */
function previousRoute(): RouteLocationResolved | null {
    const back = (window.history.state as {back?: string | null} | null)?.back;
    if (!back) return null;
    const resolved = router.resolve(back);
    return resolved.matched.length > 0 && resolved.name !== 'not-found' ? resolved : null;
}

/**
 * Revient à « to » après un formulaire : recule dans l'historique si c'est la page précédente
 * (même route, mêmes paramètres : la liste garde ses filtres et son défilement), sinon remplace.
 * @param to page à retrouver
 */
export function returnTo(to: RouteTarget) {
    const previous = previousRoute();
    const target = router.resolve(to);
    if (previous && previous.name === target.name && previous.path === target.path) return router.back();
    return router.replace(to);
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

/** Remplace des paramètres d'adresse de la page courante (ex. { salle: '12' }), sans ajout à l'historique. */
export function replaceQuery(query: Record<string, string>) {
    const current = router.currentRoute.value;
    return router.replace({path: current.path, query: {...current.query, ...query}, hash: current.hash});
}

/** Pages où l'on ne revient pas après la connexion ou le choix de la salle. */
const afterLoginExcluded: readonly RouteNames[] = ['login', 'room-choice', 'not-found'];

/** Page demandée avant la connexion (?redirect=), si c'est une page de l'application ; sinon null. */
function requestedRoute(): RouteLocationResolved | null {
    const redirect = router.currentRoute.value.query.redirect;
    if (typeof redirect !== 'string' || !redirect.startsWith('/') || redirect.startsWith('//')) return null;
    const resolved = router.resolve(redirect);
    return resolved.matched.length > 0 && !afterLoginExcluded.includes(resolved.name as RouteNames) ? resolved : null;
}

/**
 * Adresse d'une route nommée (ex. pour donner à Keycloak la page où revenir).
 * @param to cible typée
 */
export function hrefOf(to: RouteTarget): string {
    return router.resolve(to).href;
}

/**
 * Page où Keycloak ramène après la connexion : la page demandée avant (?redirect=), sinon le tableau
 * de bord. Un formateur sans salle passe ensuite par le choix de la salle (guards.ts).
 */
export function afterLoginHref(): string {
    return requestedRoute()?.fullPath ?? hrefOf({name: 'home'});
}

/**
 * Après le choix de la salle (formateur) : la page demandée avant (?redirect=), si c'est une page
 * de l'application ; sinon le tableau de bord.
 * @param query paramètres ajoutés à l'adresse (ex. { salle: '12' } après le choix de la salle)
 */
export function navigateAfterLogin(query: Record<string, string> = {}) {
    const resolved = requestedRoute();
    if (resolved) {
        return router.replace({path: resolved.path, query: {...resolved.query, ...query}, hash: resolved.hash});
    }
    return router.replace({name: 'home', query});
}
