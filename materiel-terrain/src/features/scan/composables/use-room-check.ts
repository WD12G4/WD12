/**
 * Bilan de la salle courante : matériels déclarés dans la salle, vus pendant la reconnaissance, manquants.
 * Les numéros vus sont gardés par salle sur le téléphone, pour reprendre une reconnaissance interrompue.
 * Utilisé par : features/scan/views/scan-view.vue
 */
import {computed, ref, type Ref} from 'vue';
import type {Room} from '../../../models/room.model.ts';
import {readStored, writeStored} from '../../../services/local-store.ts';
import {useInventory} from '../../inventory/composables/use-inventory.ts';
import {serialKey} from '../lib/identify.ts';

/** Numéros de série vus, par salle : { "3": { "SS4SG0548": "2026-11-05T01:10:00.000Z" } }. */
type SeenBySerial = Record<string, Record<string, string>>;

const SEEN_KEY = 'seen';

//------------------ DATAS -----------------------------------
const seen = ref<SeenBySerial>(readStored<SeenBySerial>(SEEN_KEY, {}));
const {materials} = useInventory();

//------------------ FUNCTIONS -------------------------------
/** Note qu'un matériel a été vu dans une salle. */
function markSeen(roomId: number, serialNumber: string): void {
    const key = String(roomId);
    seen.value = {...seen.value, [key]: {...seen.value[key], [serialKey(serialNumber)]: new Date().toISOString()}};
    writeStored(SEEN_KEY, seen.value);
}

/** Recommence la reconnaissance d'une salle. */
function resetRoom(roomId: number): void {
    const others = {...seen.value};
    delete others[String(roomId)];
    seen.value = others;
    writeStored(SEEN_KEY, seen.value);
}

/**
 * Bilan d'une salle.
 * @param room salle courante (réactive), ou null
 * @returns matériels attendus, manquants, nombre de vus, et les opérations
 */
export function useRoomCheck(room: Ref<Room | null>) {
    const seenInRoom = computed(() => (room.value ? seen.value[String(room.value.id)] ?? {} : {}));
    /** Matériels déclarés dans la salle (hors archivés). */
    const expected = computed(() => materials.value.filter((material) =>
        !material.archived && room.value !== null && material.room?.id === room.value.id));
    /** Déclarés dans la salle mais pas encore vus. */
    const missing = computed(() => expected.value.filter((material) =>
        !(serialKey(material.serialNumber) in seenInRoom.value)));
    const seenCount = computed(() => Object.keys(seenInRoom.value).length);

    return {expected, missing, seenCount, markSeen, resetRoom};
}
