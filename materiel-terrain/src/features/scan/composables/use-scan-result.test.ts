import {computed, ref} from 'vue';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import type {Material} from '../../../models/material.model.ts';
import type {Room} from '../../../models/room.model.ts';
import {serialKey} from '../lib/identify.ts';

const room10: Room = {id: 1, version: 0, number: 10, building: {id: 1, version: 0, number: 101}};
const inRoom10: Material = {
    id: 1, version: 0, serialNumber: 'SS4SG0548', materialType: 'UC', endAvailabilityDate: null, room: room10,
    archived: false,
};

vi.mock('../../inventory/composables/use-inventory.ts', () => ({
    useInventory: () => ({
        findBySerial: (serial: string) => (serialKey(serial) === serialKey(inRoom10.serialNumber) ? inRoom10 : null),
        roomById: () => room10,
    }),
}));
const mocks = vi.hoisted(() => ({
    register: vi.fn(),
    /** Même forme que l'erreur réelle : le matériel du serveur. */
    MaterialConflictError: class extends Error {
        readonly server: unknown;
        constructor(server: unknown) {
            super('conflit');
            this.server = server;
        }
    },
}));

vi.mock('../../inventory/composables/use-field-actions.ts', () => ({
    useFieldActions: () => ({relocate: vi.fn(), register: mocks.register}),
    MaterialConflictError: mocks.MaterialConflictError,
}));
vi.mock('../../location/composables/use-current-room.ts', () => ({
    useCurrentRoom: () => ({currentRoom: computed(() => room10)}),
}));
vi.mock('./use-room-check.ts', () => ({useRoomCheck: () => ({markSeen: vi.fn()})}));
vi.mock('./use-scan-history.ts', () => ({useScanHistory: () => ({record: () => 1, annotate: vi.fn(), entries: ref([])})}));
vi.mock('../lib/feedback.ts', () => ({playTone: vi.fn()}));

const LABEL = 'SGY12.SS4SG0548\nSS4SG0548  2CF05DC90096 2020-057 1300157350 10/2024 7021-146027780';

describe("useScanResult : c'est l'agent qui ferme le résultat", () => {
    beforeEach(() => vi.useFakeTimers());
    afterEach(() => vi.useRealTimers());

    it('garde un résultat conforme affiché, lecture suspendue, sans limite de temps', async () => {
        const {useScanResult} = await import('./use-scan-result.ts');
        const {outcome, paused, handleCode} = useScanResult();

        handleCode(LABEL, 'camera');
        vi.advanceTimersByTime(60_000);

        expect(outcome.value?.verdict).toBe('conform');
        expect(paused.value).toBe(true);
    });

    it('reprend la lecture quand l\'agent ferme le résultat', async () => {
        const {useScanResult} = await import('./use-scan-result.ts');
        const {outcome, paused, handleCode, close} = useScanResult();

        handleCode(LABEL, 'camera');
        close();

        expect(outcome.value).toBeNull();
        expect(paused.value).toBe(false);
    });
});

describe("useScanResult : numéro déjà pris en compte par un autre agent", () => {
    it('montre le matériel du serveur et son verdict au lieu de créer un doublon', async () => {
        const {useScanResult} = await import('./use-scan-result.ts');
        const {outcome, handleCode, registerMaterial} = useScanResult();
        handleCode('SS4SG0566', 'manual');
        expect(outcome.value?.verdict).toBe('unknown');
        const server: Material = {...inRoom10, id: 8, serialNumber: 'SS4SG0566'};
        mocks.register.mockRejectedValue(new mocks.MaterialConflictError(server));

        await registerMaterial({serialNumber: 'SS4SG0566', materialType: 'UC', endAvailabilityDate: '2030-01-01', roomId: 1});

        expect(outcome.value?.verdict).toBe('conform');
        expect(outcome.value?.material?.id).toBe(8);
    });
});
