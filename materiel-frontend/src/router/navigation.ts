/**
 * Navigation par programme, typée. Copié du projet du cours (vue1).
 * Toujours naviguer vers une route nommée ({ name, params }) : jamais router.push('/chemin').
 * Fonctions pour une navigation cohérente :
 * - replaceWith / returnTo : après un formulaire, il ne reste pas dans l'historique ;
 * - previousRoute : page précédente (Vue Router range son chemin dans history.state.back), utilisée
 *   aussi par le lien « ← Retour » (back-link.ts) ;
 * - afterLoginHref / navigateAfterLogin : après la connexion (Keycloak, qui joue MinDef Connect) et le
 *   choix de la salle, retour à la page demandée ;
 * - hrefOf : adresse d'une route, pour les redirections de Keycloak.
 * Utilisé par : les vues, router/back-link.ts
 */
import type {RouteLocationResolved} from 'vue-router';
import { router } from './index.ts';
import type { RouteNames, RouteTarget } from './route-types.ts';

/**
 * Remplace la page courante, qui ne reste pas dans l'historique (ex. formulaire envoyé).
 * @param to cible typée
 */
export function replaceWith(to: RouteTarget) {
    return router.replace(to);
}

/** Page précédente dans l'historique du navigateur, si c'est une page de l'application. */
export function previousRoute(): RouteLocationResolved | null {
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
