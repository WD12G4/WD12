import {beforeEach, describe, expect, it, vi} from 'vitest';
import type {Material} from '../../../models/material.model.ts';
import type {PendingAction} from '../../../models/sync.model.ts';
import {ApiError} from '../../../services/api-error.ts';

const mocks = vi.hoisted(() => ({
    reconnect: vi.fn(),
    findById: vi.fn(),
    assignRoom: vi.fn(),
    create: vi.fn(),
    findAll: vi.fn(),
    storedQueue: [] as unknown[],
}));

vi.mock('../../../shared/composables/use-auth.ts', () => ({useAuth: () => ({reconnect: mocks.reconnect})}));
vi.mock('../../../shared/composables/use-network.ts', () => ({useNetwork: () => ({markReachable: vi.fn()})}));
vi.mock('../../../services/materials.services.ts', () => ({
    materialsServices: {findById: mocks.findById, assignRoom: mocks.assignRoom, create: mocks.create, findAll: mocks.findAll},
}));
vi.mock('../../../services/rooms.services.ts', () => ({roomsServices: {findAll: vi.fn().mockResolvedValue([])}}));
vi.mock('../../../services/local-store.ts', () => ({
    readStored: (key: string, fallback: unknown) => (key === 'queue' ? mocks.storedQueue : fallback),
    writeStored: vi.fn(),
    removeStored: vi.fn(),
}));

/** Matériel tel que relu sur le serveur, dans la salle 3. */
const serverMaterial: Material = {
    id: 5, version: 0, serialNumber: 'PRJ-EPSON-001', materialType: 'PROJECTOR', endAvailabilityDate: null,
    room: {id: 3, version: 0, number: 20, building: {id: 2, version: 0, number: 102}}, archived: false,
};

/** Relocalisation de PRJ-EPSON-001 de la salle 3 vers la salle 1. */
const relocation = {
    kind: 'relocate', serialNumber: 'PRJ-EPSON-001', materialId: 5, roomId: 1, request: null, baseRoomId: 3,
} as const;

/** Prise en compte hors ligne de SS4SG0566 dans la salle 3 (matériel provisoire -1). */
const creation = {
    kind: 'create', serialNumber: 'SS4SG0566', materialId: -1, roomId: 3, baseRoomId: null,
    request: {serialNumber: 'SS4SG0566', materialType: 'UC', endAvailabilityDate: null, roomId: 3, archived: false},
} as const;

/** Le même numéro, créé entre-temps par un autre agent (casse différente), dans une salle donnée. */
function createdElsewhere(roomId: number): Material {
    return {...serverMaterial, id: 8, serialNumber: 'ss4sg0566', materialType: 'UC', room: {...serverMaterial.room!, id: roomId}};
}

/**
 * Charge un module neuf (la file est lue au chargement). ApiError est rechargée avec lui :
 * la file la reconnaît par instanceof, il faut donc la même classe que celle du module chargé.
 */
async function loadQueue() {
    const {useSyncQueue} = await import('./use-sync-queue.ts');
    const {useInventory} = await import('./use-inventory.ts');
    const {ApiError: LoadedApiError} = await import('../../../services/api-error.ts');
    return {...useSyncQueue(), inventory: useInventory(), LoadedApiError};
}

describe('useSyncQueue : session expirée et refus', () => {
    beforeEach(() => {
        vi.resetModules();
        vi.clearAllMocks();
        mocks.storedQueue = [];
        mocks.reconnect.mockResolvedValue('ok');
        mocks.findById.mockResolvedValue(serverMaterial);
    });

    it("garde l'action en attente quand la session expire pendant le rejeu", async () => {
        const {enqueue, flush, queue, readyActions, LoadedApiError} = await loadQueue();
        mocks.assignRoom.mockRejectedValue(new LoadedApiError(401));
        enqueue(relocation);

        await flush();

        expect(queue.value).toHaveLength(1);
        expect(queue.value[0].error).toBeNull();
        expect(readyActions.value).toHaveLength(1);
    });

    it("n'envoie rien tant que la session n'est pas rétablie", async () => {
        mocks.reconnect.mockResolvedValue('expired');
        const {enqueue, flush, readyActions} = await loadQueue();
        enqueue(relocation);

        await flush();

        expect(mocks.findById).not.toHaveBeenCalled();
        expect(readyActions.value).toHaveLength(1);
    });

    it('marque refusée une action rejetée par une règle métier', async () => {
        const {enqueue, flush, queue, readyActions, LoadedApiError} = await loadQueue();
        mocks.assignRoom.mockRejectedValue(new LoadedApiError(400, 'Le matériel ne peut pas être affecté à une salle.'));
        enqueue(relocation);

        await flush();

        expect(queue.value[0].error).toBe('Le matériel ne peut pas être affecté à une salle.');
        expect(readyActions.value).toHaveLength(0);
    });

    it('remet en attente, au chargement, une action refusée à tort pour session expirée', async () => {
        const stored = (id: string, error: string): Partial<PendingAction> => ({
            ...relocation, id, createdAt: '2026-11-05T03:00:00.000Z', error,
        });
        mocks.storedQueue = [stored('a', new ApiError(401).message), stored('b', 'Refus métier')];

        const {queue, readyActions} = await loadQueue();

        expect(queue.value.map((action) => action.error)).toEqual([null, 'Refus métier']);
        expect(readyActions.value.map((action) => action.id)).toEqual(['a']);
    });
});

describe("useSyncQueue : prise en compte d'un numéro déjà créé par un autre agent", () => {
    beforeEach(() => {
        vi.resetModules();
        vi.clearAllMocks();
        mocks.storedQueue = [];
        mocks.reconnect.mockResolvedValue('ok');
    });

    /** Prise en compte faite hors ligne : matériel provisoire dans le cache, action dans la file. */
    async function offlineCreation() {
        const loaded = await loadQueue();
        loaded.inventory.upsertMaterial({...createdElsewhere(3), id: -1, serialNumber: 'SS4SG0566'});
        loaded.enqueue(creation);
        return loaded;
    }

    it('crée le matériel si le serveur ne le connaît pas', async () => {
        mocks.findAll.mockResolvedValue([]);
        mocks.create.mockResolvedValue({...createdElsewhere(3), serialNumber: 'SS4SG0566'});
        const {flush, queue} = await offlineCreation();

        await flush();

        expect(mocks.create).toHaveBeenCalledOnce();
        expect(queue.value).toHaveLength(0);
    });

    it('ne crée rien et clôt l\'action si l\'autre agent l\'a mis dans la même salle', async () => {
        mocks.findAll.mockResolvedValue([createdElsewhere(3)]);
        const {flush, queue, inventory} = await offlineCreation();

        await flush();

        expect(mocks.create).not.toHaveBeenCalled();
        expect(queue.value).toHaveLength(0);
        expect(inventory.materials.value.map((material) => material.id)).toEqual([8]);
    });

    it('passe en conflit, sans rien créer, si l\'autre agent l\'a mis ailleurs', async () => {
        mocks.findAll.mockResolvedValue([createdElsewhere(1)]);
        const {flush, queue, inventory} = await offlineCreation();

        await flush();

        expect(mocks.create).not.toHaveBeenCalled();
        expect(queue.value[0].conflict?.id).toBe(8);
        expect(inventory.materials.value.map((material) => material.id)).toEqual([8]);
    });

    it('ne garde jamais deux fiches pour un même numéro au rechargement du parc', async () => {
        mocks.findAll.mockResolvedValue([createdElsewhere(3)]);
        const {inventory} = await offlineCreation();

        await inventory.download();

        expect(inventory.materials.value.map((material) => material.id)).toEqual([8]);
    });
});
