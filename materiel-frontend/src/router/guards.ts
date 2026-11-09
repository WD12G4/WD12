/**
 * Garde de route : restaure la session, renvoie vers la connexion si besoin,
 * et refuse les pages réservées à un rôle supérieur.
 * Utilisé par : router/index.ts
 * RG : RG-14 à RG-18 (droits par rôle), RG-31, RG-32
 *
 * Ce contrôle évite d'afficher des pages inutilisables : la vraie sécurité reste
 * assurée par le backend (@PreAuthorize).
 */
import type {RouteLocationNormalized, RouteLocationRaw, Router} from 'vue-router';
import {useAuth} from '../shared/composables/use-auth.ts';
import {useFlashMessage} from '../shared/composables/use-flash-message.ts';

const {isAuthenticated, restoreSession, hasRole, onSessionExpired} = useAuth();
const {show} = useFlashMessage();

/**
 * Décide si la navigation est permise.
 * @param to route demandée
 * @returns true pour laisser passer, ou la route vers laquelle rediriger
 */
async function checkAccess(to: RouteLocationNormalized): Promise<true | RouteLocationRaw> {
    // Au premier chargement, le jeton n'est pas encore en mémoire : on le récupère via le cookie.
    await restoreSession();

    if (to.meta.public) {
        // Un utilisateur déjà connecté n'a rien à faire sur la page de connexion.
        return isAuthenticated.value ? {name: 'home'} : true;
    }
    if (!isAuthenticated.value) {
        // La page demandée est gardée : après la connexion, on y revient (navigation.ts, navigateAfterLogin).
        return to.fullPath === '/' ? {name: 'login'} : {name: 'login', query: {redirect: to.fullPath}};
    }
    if (!to.meta.roles.some(hasRole)) {
        show("Vous n'avez pas accès à cette page.", 'error');
        return {name: 'home'};
    }
    return true;
}

/**
 * Installe la garde sur le router et la redirection en cas de session expirée.
 * Les redirections utilisent des objets { name } : jamais de chemin en chaîne.
 * @param router instance créée dans index.ts
 */
export function installGuards(router: Router): void {
    router.beforeEach(checkAccess);

    onSessionExpired(() => {
        show('Votre session a expiré. Reconnectez-vous.', 'error');
        void router.push({name: 'login', query: {redirect: router.currentRoute.value.fullPath}});
    });
}
