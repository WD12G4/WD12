/**
 * File des actions faites sans réseau, gardée sur le téléphone et rejouée vers l'API dans l'ordre.
 * Une action refusée par l'API (règle métier, droits) est marquée en erreur et n'est plus rejouée :
 * l'agent la réessaie ou l'abandonne depuis l'écran Synchronisation.
 * Utilisé par : App.vue, use-field-actions.ts, use-synchronize.ts, features/inventory/views/sync-view.vue
 */
import {computed, ref} from 'vue';
import type {Material} from '../../../models/material.model.ts';
import type {PendingAction} from '../../../models/sync.model.ts';
import {isUnreachable} from '../../../services/api-error.ts';
import {materialsServices} from '../../../services/materials.services.ts';
import {readStored, writeStored} from '../../../services/local-store.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {toErrorMessage} from '../../../shared/composables/use-async-state.ts';
import {useNetwork} from '../../../shared/composables/use-network.ts';
import {useInventory} from './use-inventory.ts';

const QUEUE_KEY = 'queue';

//------------------ DATAS -----------------------------------
const queue = ref<PendingAction[]>(readStored<PendingAction[]>(QUEUE_KEY, []));
const flushing = ref(false);
const {reconnect} = useAuth();
const {markReachable} = useNetwork();
const inventory = useInventory();

//------------------ COMPUTED --------------------------------
/** Actions à rejouer automatiquement (sans erreur de l'API). */
const readyActions = computed(() => queue.value.filter((action) => action.error === null));
const pendingCount = computed(() => queue.value.length);
const blockedCount = computed(() => queue.value.length - readyActions.value.length);

//------------------ FUNCTIONS -------------------------------
function persist(): void {
    writeStored(QUEUE_KEY, queue.value);
}

/** Identifiant unique ; randomUUID n'existe qu'en contexte sécurisé (HTTPS ou localhost). */
function newId(): string {
    return typeof crypto.randomUUID === 'function' ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`;
}

/** Ajoute une action à la file. */
function enqueue(action: Omit<PendingAction, 'id' | 'createdAt' | 'error'>): void {
    queue.value.push({...action, id: newId(), createdAt: new Date().toISOString(), error: null});
    persist();
}

/** Retire une action de la file. */
function remove(id: string): void {
    queue.value = queue.value.filter((action) => action.id !== id);
    persist();
}

/** Reporte une relocalisation sur la prise en compte en attente d'un matériel provisoire. */
function retargetPendingCreate(provisionalId: number, roomId: number | null): void {
    const action = queue.value.find((pending) => pending.kind === 'create' && pending.materialId === provisionalId);
    if (action?.request) {
        action.roomId = roomId;
        action.request = {...action.request, roomId};
        persist();
    }
}

/** Envoie une action à l'API. */
function send(action: PendingAction): Promise<Material> {
    return action.kind === 'create' && action.request
        ? materialsServices.create(action.request)
        : materialsServices.assignRoom(action.materialId, action.roomId);
}

/** Rejoue les actions prêtes, dans l'ordre. S'arrête dès que l'API est injoignable. */
async function flush(): Promise<void> {
    if (flushing.value || readyActions.value.length === 0) return;
    flushing.value = true;
    try {
        if (!(await reconnect())) {
            markReachable(false);
            return;
        }
        for (const action of [...readyActions.value]) {
            try {
                const saved = await send(action);
                if (action.kind === 'create') inventory.removeMaterial(action.materialId);
                inventory.upsertMaterial(saved);
                remove(action.id);
                markReachable(true);
            } catch (error) {
                if (isUnreachable(error)) {
                    markReachable(false);
                    break;
                }
                action.error = toErrorMessage(error);
                persist();
            }
        }
    } finally {
        flushing.value = false;
    }
}

/** Remet une action refusée dans le circuit, puis tente de l'envoyer. */
async function retry(id: string): Promise<void> {
    const action = queue.value.find((pending) => pending.id === id);
    if (action) action.error = null;
    persist();
    await flush();
}

/** Abandonne une action ; un matériel provisoire jamais créé disparaît du cache. */
function discard(id: string): void {
    const action = queue.value.find((pending) => pending.id === id);
    if (action?.kind === 'create') inventory.removeMaterial(action.materialId);
    remove(id);
}

/**
 * Accès à la file partagée.
 * @returns la file, ses compteurs et ses opérations
 */
export function useSyncQueue() {
    return {
        queue, flushing, readyActions, pendingCount, blockedCount,
        enqueue, retargetPendingCreate, flush, retry, discard,
    };
}
