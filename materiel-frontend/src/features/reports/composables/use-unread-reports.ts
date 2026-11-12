/**
 * Nombre de remontées à traiter (non traitées), affiché en pastille rouge à côté du lien « Remontées »
 * du menu pour le gestionnaire (RG-23 : notification dans l'application, pas de mail).
 * Le formateur n'est pas concerné : il ne traite pas les remontées.
 * L'état est partagé au niveau du module : App.vue lance le chargement, le menu affiche, les vues des
 * remontées rechargent le compteur après une action.
 * Utilisé par : App.vue, shared/composables/use-nav-links.ts, report-view.vue, report-create-view.vue
 */
import {ref, watch} from 'vue';
import {reportsServices} from '../../../services/reports.services.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';

//------------------ DATAS -----------------------------------
const unreadCount = ref(0);
/** Vrai une fois la surveillance de la connexion installée (une seule fois pour toute l'application). */
let watching = false;

//------------------ FUNCTIONS -------------------------------
/**
 * Recharge le compteur pour un gestionnaire, 0 sinon.
 * Une erreur n'empêche pas d'utiliser l'application : la pastille ne s'affiche simplement pas.
 */
async function refresh(): Promise<void> {
    const {hasRole} = useAuth();
    unreadCount.value = hasRole('MANAGER') ? await reportsServices.countUnhandled().catch(() => 0) : 0;
}

/**
 * Nombre de remontées non traitées, rechargé à chaque connexion (changement d'utilisateur).
 * @returns unreadCount (lecture) et refresh() (recharger après une action sur les remontées)
 */
export function useUnreadReports() {
    if (!watching) {
        watching = true;
        const {currentUser} = useAuth();
        watch(() => currentUser.value?.id, refresh, {immediate: true});
    }
    return {unreadCount, refresh};
}
