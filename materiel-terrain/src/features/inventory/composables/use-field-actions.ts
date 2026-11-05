/**
 * Actions de terrain : relocaliser un matériel, prendre en compte un matériel non recensé.
 * Chaque action part tout de suite vers l'API si elle est joignable ; sinon elle est gardée dans la file
 * (use-sync-queue.ts) et le cache est mis à jour pour que l'agent voie déjà le résultat.
 * Utilisé par : features/scan/views/scan-view.vue
 * RG : RG-07 (affectation), RG-17 (réservé à MANAGER et ADMIN, vérifié par le backend)
 */
import {isProvisional, type Material, type MaterialInput} from '../../../models/material.model.ts';
import {materialFromDto, materialToRequest} from '../../../models/material.dto.ts';
import type {PendingAction} from '../../../models/sync.model.ts';
import type {Room} from '../../../models/room.model.ts';
import {roomToDto} from '../../../models/room.dto.ts';
import {isUnreachable} from '../../../services/api-error.ts';
import {materialsServices} from '../../../services/materials.services.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useNetwork} from '../../../shared/composables/use-network.ts';
import {useInventory} from './use-inventory.ts';
import {useSyncQueue} from './use-sync-queue.ts';

/** 'saved' = enregistré par l'API ; 'queued' = gardé sur le téléphone, envoyé au retour du réseau. */
type FieldActionResult = 'saved' | 'queued';

//------------------ DATAS -----------------------------------
const {reconnect} = useAuth();
const {markReachable} = useNetwork();
const {upsertMaterial, moveMaterial} = useInventory();
const {readyActions, enqueue, flush, retargetPendingCreate} = useSyncQueue();

//------------------ FUNCTIONS -------------------------------
/**
 * Envoie l'action à l'API, ou la met en file si l'API est injoignable.
 * S'il reste des actions en attente, la nouvelle passe derrière elles pour garder l'ordre du terrain.
 * @param action action à garder si besoin
 * @param direct appel direct à l'API
 * @param applyLocally mise à jour du cache quand l'action est mise en file
 * @throws ApiError si l'API refuse l'action (règle métier, droits)
 */
async function runOrQueue(action: Omit<PendingAction, 'id' | 'createdAt' | 'error'>,
                          direct: () => Promise<Material>, applyLocally: () => void): Promise<FieldActionResult> {
    if (readyActions.value.length === 0 && (await reconnect())) {
        try {
            upsertMaterial(await direct());
            markReachable(true);
            return 'saved';
        } catch (error) {
            if (!isUnreachable(error)) throw error;
            markReachable(false);
        }
    }
    enqueue(action);
    applyLocally();
    void flush();
    return 'queued';
}

/**
 * Relocalise un matériel dans la salle où il a été trouvé.
 * @param material matériel identifié
 * @param room salle courante de l'agent
 */
async function relocate(material: Material, room: Room): Promise<FieldActionResult> {
    if (isProvisional(material)) {
        // Matériel pris en compte hors ligne : on corrige la prise en compte encore en attente.
        retargetPendingCreate(material.id, room.id);
        moveMaterial(material.id, room);
        return 'queued';
    }
    return runOrQueue(
        {kind: 'relocate', serialNumber: material.serialNumber, materialId: material.id, roomId: room.id, request: null},
        () => materialsServices.assignRoom(material.id, room.id),
        () => moveMaterial(material.id, room),
    );
}

/**
 * Prend en compte un matériel non recensé, dans la salle où il a été trouvé.
 * @param input saisie du formulaire
 * @param room salle courante, ou null (en stock)
 */
async function register(input: MaterialInput, room: Room | null): Promise<FieldActionResult> {
    const request = materialToRequest({...input, roomId: room?.id ?? null});
    // Identifiant provisoire négatif, unique sur le téléphone, remplacé par celui de l'API à l'envoi.
    const provisional = materialFromDto({
        ...request, id: -Date.now(), version: 0, room: room ? roomToDto(room) : null,
    });
    return runOrQueue(
        {kind: 'create', serialNumber: request.serialNumber, materialId: provisional.id, roomId: request.roomId, request},
        () => materialsServices.create(request),
        () => upsertMaterial(provisional),
    );
}

/**
 * Accès aux actions de terrain.
 * @returns relocate() et register()
 */
export function useFieldActions() {
    return {relocate, register};
}

export type {FieldActionResult};
