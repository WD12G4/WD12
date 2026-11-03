/**
 * Nombre de remontées non lues, chargé à la connexion d'un gestionnaire (RG-23 : pop-up à la connexion,
 * pas de mail). Le formateur n'est pas concerné : il ne consulte pas les remontées.
 * Utilisé par : App.vue
 */
import {computed, ref, watch} from 'vue';
import {reportsServices} from '../../../services/reports.services.mock.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';

/**
 * Charge le compteur pour un gestionnaire, 0 sinon.
 * Une erreur n'empêche pas d'utiliser l'application : la pop-up ne s'affiche simplement pas.
 * @param isManager vrai si l'utilisateur connecté est gestionnaire ou administrateur
 */
async function fetchUnreadCount(isManager: boolean): Promise<number> {
    return isManager ? reportsServices.countUnread().catch(() => 0) : 0;
}

/**
 * Surveille la connexion et charge le nombre de remontées non lues.
 * @returns unreadCount, visible (la pop-up est à afficher) et dismiss() (la fermer)
 */
export function useUnreadReports() {
    const {currentUser, hasRole} = useAuth();
    const unreadCount = ref(0);
    const dismissed = ref(false);
    const visible = computed(() => !dismissed.value && unreadCount.value > 0);

    // Chaque nouvelle connexion (changement d'utilisateur) recharge le compteur et réaffiche la pop-up.
    watch(() => currentUser.value?.id, async () => {
        dismissed.value = false;
        unreadCount.value = await fetchUnreadCount(hasRole('MANAGER'));
    }, {immediate: true});

    return {unreadCount, visible, dismiss: () => { dismissed.value = true; }};
}
