/**
 * Garde de route : restaure la session (en ligne, ou hors ligne sur le dernier profil connu),
 * renvoie vers la connexion si besoin. La vraie sécurité reste assurée par le backend (@PreAuthorize).
 * Utilisé par : router/index.ts
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
    await restoreSession();

    if (to.meta.public) {
        return isAuthenticated.value ? {name: 'scan'} : true;
    }
    if (!isAuthenticated.value) {
        return {name: 'login'};
    }
    if (!to.meta.roles.some(hasRole)) {
        show("Vous n'avez pas accès à cette page.", 'error');
        return {name: 'scan'};
    }
    return true;
}

/**
 * Installe la garde sur le router et la redirection en cas de session expirée.
 * @param router instance créée dans index.ts
 */
export function installGuards(router: Router): void {
    router.beforeEach(checkAccess);

    onSessionExpired(() => {
        show('Votre session a expiré. Reconnectez-vous : les actions en attente sont conservées.', 'error');
        void router.push({name: 'login'});
    });
}
