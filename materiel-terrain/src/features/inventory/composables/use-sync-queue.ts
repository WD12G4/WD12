/**
 * File des actions faites sans réseau, gardée sur le téléphone et rejouée vers l'API dans l'ordre.
 * Avant chaque relocalisation, le matériel est relu sur le serveur (room-conflict.ts) : s'il a été déplacé
 * entre-temps par quelqu'un d'autre, l'action passe en conflit au lieu d'écraser son travail.
 * Une action en conflit ou refusée par l'API n'est plus rejouée automatiquement : l'agent arbitre
 * depuis l'écran Synchronisation (use-conflicts.ts, retry, discard).
 * Utilisé par : App.vue, use-field-actions.ts, use-synchronize.ts, use-conflicts.ts, sync-view.vue
 */
import {computed, ref} from 'vue';
import {isProvisional, type Material} from '../../../models/material.model.ts';
import {materialToDto} from '../../../models/material.dto.ts';
import type {PendingAction} from '../../../models/sync.model.ts';
import {isUnreachable} from '../../../services/api-error.ts';
import {materialsServices} from '../../../services/materials.services.ts';
import {readStored, writeStored} from '../../../services/local-store.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {toErrorMessage} from '../../../shared/composables/use-async-state.ts';
import {useFlashMessage} from '../../../shared/composables/use-flash-message.ts';
import {useNetwork} from '../../../shared/composables/use-network.ts';
import {serialKey} from '../../scan/lib/identify.ts';
import {checkRoom, MaterialConflictError} from '../lib/room-conflict.ts';
import {useInventory} from './use-inventory.ts';

const QUEUE_KEY = 'queue';

/** Champs fournis par l'appelant ; les autres sont remplis à l'ajout. */
type NewAction = Omit<PendingAction, 'id' | 'createdAt' | 'error' | 'conflict'>;

//------------------ DATAS -----------------------------------
// Les actions d'une version précédente n'ont pas de champ conflict.
const queue = ref<PendingAction[]>(readStored<PendingAction[]>(QUEUE_KEY, [])
    .map((action) => ({...action, conflict: action.conflict ?? null})));
const flushing = ref(false);
const {reconnect} = useAuth();
const {markReachable} = useNetwork();
const {show} = useFlashMessage();
const inventory = useInventory();

//------------------ COMPUTED --------------------------------
/** Actions à rejouer automatiquement (ni refusées, ni en conflit). */
const readyActions = computed(() => queue.value.filter((action) => action.error === null && action.conflict === null));
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

function enqueue(action: NewAction): void {
    queue.value.push({...action, id: newId(), createdAt: new Date().toISOString(), error: null, conflict: null});
    persist();
}

function remove(id: string): void {
    queue.value = queue.value.filter((action) => action.id !== id);
    persist();
}

/** Modifie une action (arbitrage d'un conflit, nouvelle tentative). */
function updateAction(id: string, changes: Partial<PendingAction>): void {
    queue.value = queue.value.map((action) => (action.id === id ? {...action, ...changes} : action));
    persist();
}

/** Reporte un changement de salle sur la prise en compte en attente d'un matériel provisoire. */
function retargetPendingCreate(provisionalId: number, roomId: number | null): void {
    const action = queue.value.find((pending) => pending.kind === 'create' && pending.materialId === provisionalId);
    if (action?.request) updateAction(action.id, {roomId, request: {...action.request, roomId}});
}

/** Prise en compte refusée : si le numéro existe désormais sur le serveur, un autre agent l'a créé (conflit). */
async function findCreatedElsewhere(serialNumber: string): Promise<Material | null> {
    await inventory.download();
    return inventory.materials.value.find((material) =>
        !isProvisional(material) && serialKey(material.serialNumber) === serialKey(serialNumber)) ?? null;
}

/**
 * Envoie une action à l'API, après avoir vérifié que le serveur ne la contredit pas.
 * @throws MaterialConflictError si quelqu'un a modifié le matériel entre-temps, ApiError sinon
 */
async function send(action: PendingAction): Promise<Material> {
    if (action.kind === 'create' && action.request) {
        try {
            return await materialsServices.create(action.request);
        } catch (error) {
            const existing = isUnreachable(error) ? null : await findCreatedElsewhere(action.serialNumber);
            throw existing ? new MaterialConflictError(existing) : error;
        }
    }
    const server = await materialsServices.findById(action.materialId);
    const check = checkRoom(server, action.baseRoomId, action.roomId);
    if (check === 'conflict') throw new MaterialConflictError(server);
    return check === 'already-done' ? server : materialsServices.assignRoom(action.materialId, action.roomId);
}

/** Rejoue les actions prêtes, dans l'ordre. S'arrête dès que l'API est injoignable. */
async function flush(): Promise<void> {
    if (flushing.value || readyActions.value.length === 0) return;
    flushing.value = true;
    let conflicts = 0;
    try {
        if (!(await reconnect())) return markReachable(false);
        for (const action of [...readyActions.value]) {
            try {
                const saved = await send(action);
                if (action.kind === 'create') inventory.removeMaterial(action.materialId);
                inventory.upsertMaterial(saved);
                remove(action.id);
                markReachable(true);
            } catch (error) {
                if (isUnreachable(error)) return markReachable(false);
                // Déjà créé par un autre : le matériel provisoire disparaît, le cache montre celui du serveur.
                if (error instanceof MaterialConflictError && action.kind === 'create') {
                    inventory.removeMaterial(action.materialId);
                }
                conflicts += error instanceof MaterialConflictError ? 1 : 0;
                updateAction(action.id, error instanceof MaterialConflictError
                    ? {conflict: materialToDto(error.server)} : {error: toErrorMessage(error)});
            }
        }
    } finally {
        flushing.value = false;
        if (conflicts > 0) show(`${conflicts} conflit(s) à arbitrer dans Synchronisation.`, 'error');
    }
}

/** Remet une action refusée dans le circuit, puis tente de l'envoyer. */
async function retry(id: string): Promise<void> {
    updateAction(id, {error: null});
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
        enqueue, updateAction, retargetPendingCreate, flush, retry, discard,
    };
}

export type {NewAction};
