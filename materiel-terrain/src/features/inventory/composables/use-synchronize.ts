/**
 * Synchronisation complète avec le serveur : envoi des actions en attente, puis parc à jour.
 * Utilisé par : App.vue (connexion, retour du réseau, retour au premier plan), features/inventory/views/sync-view.vue
 */
import {isUnreachable} from '../../../services/api-error.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useNetwork} from '../../../shared/composables/use-network.ts';
import {useInventory} from './use-inventory.ts';
import {useSyncQueue} from './use-sync-queue.ts';

//------------------ DATAS -----------------------------------
const {reconnect} = useAuth();
const {markReachable} = useNetwork();
const inventory = useInventory();
const {readyActions, flush} = useSyncQueue();

//------------------ FUNCTIONS -------------------------------
/**
 * Rejoue la file, puis recharge le parc depuis l'API.
 * Les relocalisations encore en attente sont réappliquées au cache pour ne pas les perdre de vue.
 * @returns vrai si le parc a été rechargé
 */
async function synchronize(): Promise<boolean> {
    await flush();
    try {
        if (!(await reconnect())) {
            markReachable(false);
            return false;
        }
        await inventory.download();
        // Les actions en conflit ou refusées ne sont pas réappliquées : le cache montre l'état du serveur.
        readyActions.value.filter((action) => action.kind === 'relocate').forEach((action) => {
            inventory.moveMaterial(action.materialId, action.roomId === null ? null : inventory.roomById(action.roomId));
        });
        markReachable(true);
        return true;
    } catch (error) {
        markReachable(!isUnreachable(error));
        return false;
    }
}

/**
 * Accès à la synchronisation complète.
 * @returns synchronize()
 */
export function useSynchronize() {
    return {synchronize};
}
