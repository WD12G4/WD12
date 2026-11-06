import {ref} from 'vue';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import type {PendingAction} from '../../../models/sync.model.ts';

const mocks = vi.hoisted(() => ({
    stored: {} as Record<string, unknown>,
    findAll: vi.fn(),
    enqueue: vi.fn(),
    flush: vi.fn(),
    readyActions: null as unknown as {value: Partial<PendingAction>[]},
}));

vi.mock('../../../services/local-store.ts', () => ({
    readStored: (key: string, fallback: unknown) => mocks.stored[key] ?? fallback,
    writeStored: (key: string, value: unknown) => {
        mocks.stored[key] = value;
    },
}));
vi.mock('../../../services/sightings.services.ts', () => ({sightingsServices: {findAll: mocks.findAll}}));
vi.mock('../../../shared/composables/use-auth.ts', () => ({useAuth: () => ({currentUser: ref({username: 'agent1'})})}));
vi.mock('./use-sync-queue.ts', () => ({
    useSyncQueue: () => ({readyActions: mocks.readyActions, enqueue: mocks.enqueue, flush: mocks.flush}),
}));

/** Charge un module neuf : les constats sont relus au chargement. */
async function loadSightings() {
    const {useSightings} = await import('./use-sightings.ts');
    return useSightings();
}

describe('useSightings : constats partagés entre les téléphones', () => {
    beforeEach(() => {
        vi.resetModules();
        vi.clearAllMocks();
        mocks.stored = {};
        mocks.readyActions = ref([]);
    });

    it('note un scan tout de suite et le met en file pour les autres agents', async () => {
        const {markSeen, seenInRoom} = await loadSightings();

        markSeen(1, 'ss4sg0548');

        expect(seenInRoom(1).SS4SG0548).toMatchObject({by: 'agent1'});
        expect(mocks.enqueue).toHaveBeenCalledWith(expect.objectContaining({kind: 'sight', serialNumber: 'ss4sg0548', roomId: 1}));
        expect(mocks.flush).toHaveBeenCalled();
    });

    it("reprend les constats du serveur, faits par d'autres agents", async () => {
        mocks.findAll.mockResolvedValue([
            {id: 1, serialNumber: 'UC-DELL-001', roomId: 1, seenAt: '2026-11-05T01:00:00.000Z', seenBy: 'agent2'},
        ]);
        const {download, seenInRoom} = await loadSightings();

        await download();

        expect(seenInRoom(1)['UC-DELL-001']).toEqual({at: '2026-11-05T01:00:00.000Z', by: 'agent2'});
    });

    it('réapplique, dans l\'ordre, les constats encore en file par-dessus ceux du serveur', async () => {
        mocks.findAll.mockResolvedValue([
            {id: 1, serialNumber: 'UC-DELL-001', roomId: 1, seenAt: '2026-11-05T01:00:00.000Z', seenBy: 'agent2'},
            {id: 2, serialNumber: 'PRJ-EPSON-001', roomId: 3, seenAt: '2026-11-05T01:00:00.000Z', seenBy: 'agent2'},
        ]);
        mocks.readyActions.value = [
            {kind: 'clear-room', roomId: 3, serialNumber: ''},
            {kind: 'sight', roomId: 1, serialNumber: 'SS4SG0548', seenAt: '2026-11-05T02:00:00.000Z'},
        ];
        const {download, seenInRoom} = await loadSightings();

        await download();

        expect(Object.keys(seenInRoom(1)).sort()).toEqual(['SS4SG0548', 'UC-DELL-001']);
        expect(seenInRoom(3)).toEqual({});
    });

    it("partage les scans d'une version précédente, gardés seulement sur le téléphone", async () => {
        mocks.stored.seen = {'1': {'UC-DELL-001': '2026-11-05T01:00:00.000Z'}};

        const {seenInRoom} = await loadSightings();

        expect(seenInRoom(1)['UC-DELL-001']).toEqual({at: '2026-11-05T01:00:00.000Z', by: null});
        expect(mocks.enqueue).toHaveBeenCalledWith(expect.objectContaining({
            kind: 'sight', serialNumber: 'UC-DELL-001', roomId: 1, seenAt: '2026-11-05T01:00:00.000Z',
        }));
    });
});
