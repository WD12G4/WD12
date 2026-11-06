/**
 * Synchronisation complète avec le serveur : envoi des actions en attente, puis parc et constats à jour.
 * Utilisé par : App.vue (connexion, retour du réseau, retour au premier plan, rafraîchissement régulier),
 * features/inventory/views/sync-view.vue
 */
import {isSessionExpired, isUnreachable} from '../../../services/api-error.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {toErrorMessage} from '../../../shared/composables/use-async-state.ts';
import {useNetwork} from '../../../shared/composables/use-network.ts';
import {useInventory} from './use-inventory.ts';
import {useSightings} from './use-sightings.ts';
import {useSyncQueue} from './use-sync-queue.ts';

/**
 * Issue d'une synchronisation, pour un message exact : serveur injoignable, session expirée
 * (l'agent est renvoyé vers la connexion) ou autre échec (message de l'erreur).
 */
type SyncOutcome =
    | {status: 'ok'}
    | {status: 'unreachable'}
    | {status: 'expired'}
    | {status: 'failed'; message: string};

//------------------ DATAS -----------------------------------
const {reconnect} = useAuth();
const {markReachable} = useNetwork();
const inventory = useInventory();
const sightings = useSightings();
const {readyActions, flush} = useSyncQueue();
/** Synchronisation en cours, partagée : un appel pendant qu'une autre tourne attend la même. */
let running: Promise<SyncOutcome> | null = null;

//------------------ FUNCTIONS -------------------------------
/**
 * Rejoue la file, puis recharge le parc depuis l'API.
 * Les relocalisations encore en attente sont réappliquées au cache pour ne pas les perdre de vue.
 */
function synchronize(): Promise<SyncOutcome> {
    running ??= run().finally(() => {
        running = null;
    });
    return running;
}

/** Constats partagés ; un serveur qui ne les connaît pas encore (ancienne image) ne bloque pas la synchro. */
async function downloadSightings(): Promise<void> {
    try {
        await sightings.download();
    } catch (error) {
        if (isUnreachable(error) || isSessionExpired(error)) throw error;
    }
}

async function run(): Promise<SyncOutcome> {
    await flush();
    try {
        const session = await reconnect();
        if (session !== 'ok') {
            markReachable(session !== 'unreachable');
            return {status: session};
        }
        await inventory.download();
        await downloadSightings();
        // Les actions en conflit ou refusées ne sont pas réappliquées : le cache montre l'état du serveur.
        readyActions.value.filter((action) => action.kind === 'relocate').forEach((action) => {
            inventory.moveMaterial(action.materialId, action.roomId === null ? null : inventory.roomById(action.roomId));
        });
        markReachable(true);
        return {status: 'ok'};
    } catch (error) {
        markReachable(!isUnreachable(error));
        if (isUnreachable(error)) return {status: 'unreachable'};
        if (isSessionExpired(error)) return {status: 'expired'};
        return {status: 'failed', message: toErrorMessage(error)};
    }
}

/**
 * Accès à la synchronisation complète.
 * @returns synchronize()
 */
export function useSynchronize() {
    return {synchronize};
}

export type {SyncOutcome};
