/**
 * Bilan de la salle courante : matériels déclarés dans la salle, vus pendant la reconnaissance, manquants.
 * Les numéros vus (recensés ou non) sont gardés par salle sur le téléphone, pour reprendre une reconnaissance
 * interrompue et pour le compte rendu des salles.
 * Utilisé par : features/scan/views/scan-view.vue, use-scan-result.ts, features/rooms (lecture seule)
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
/**
 * Note qu'un matériel a été vu dans une salle.
 * @param roomId salle où il a été scanné
 * @param serialNumber numéro retenu
 * @param replaces numéro lu, remplacé par serialNumber (corrigé par l'agent ou trouvé sur le serveur) : oublié
 */
function markSeen(roomId: number, serialNumber: string, replaces?: string): void {
    const key = String(roomId);
    const inRoom = {...seen.value[key]};
    if (replaces !== undefined) delete inRoom[serialKey(replaces)];
    inRoom[serialKey(serialNumber)] = new Date().toISOString();
    seen.value = {...seen.value, [key]: inRoom};
    writeStored(SEEN_KEY, seen.value);
}

/** Numéros vus dans une salle (lecture seule), avec la date du dernier scan. */
function seenInRoom(roomId: number): Readonly<Record<string, string>> {
    return seen.value[String(roomId)] ?? {};
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
    const seenHere = computed(() => (room.value ? seenInRoom(room.value.id) : {}));
    /** Matériels déclarés dans la salle (hors archivés). */
    const expected = computed(() => materials.value.filter((material) =>
        !material.archived && room.value !== null && material.room?.id === room.value.id));
    /** Déclarés dans la salle mais pas encore vus. */
    const missing = computed(() => expected.value.filter((material) =>
        !(serialKey(material.serialNumber) in seenHere.value)));
    const seenCount = computed(() => Object.keys(seenHere.value).length);

    return {expected, missing, seenCount, markSeen, resetRoom};
}

/**
 * Accès en lecture aux numéros vus, salle par salle (compte rendu des salles).
 * @returns seenInRoom()
 */
export function useSeenRecords() {
    return {seenInRoom};
}
