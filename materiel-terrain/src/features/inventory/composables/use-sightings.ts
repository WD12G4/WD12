/**
 * Constats de terrain partagés : quels numéros ont été scannés dans quelle salle, quand et par qui.
 * Un matériel constaté une fois l'est pour tout le monde. Le serveur (/api/v1/sightings) fait foi :
 * un scan est noté tout de suite sur le téléphone, puis envoyé par la file (use-sync-queue.ts), hors réseau compris.
 * Au rechargement, les constats encore en file sont réappliqués par-dessus ceux du serveur.
 * Utilisé par : use-synchronize.ts, features/scan (bilan de salle, scan), features/rooms (compte rendu)
 */
import {ref} from 'vue';
import {readStored, writeStored} from '../../../services/local-store.ts';
import {sightingsServices} from '../../../services/sightings.services.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {serialKey} from '../../scan/lib/identify.ts';
import {useSyncQueue} from './use-sync-queue.ts';

/** Dernier scan d'un numéro dans une salle : date (ISO) et agent (null s'il n'est pas connu). */
interface Seen {
    at: string;
    by: string | null;
}

/** Constats par salle puis par numéro normalisé : { "3": { "SS4SG0548": {at, by} } }. */
type SeenByRoom = Record<string, Record<string, Seen>>;

const SEEN_KEY = 'seen';

//------------------ DATAS -----------------------------------
const {currentUser} = useAuth();
const {readyActions, enqueue, flush} = useSyncQueue();
const seen = ref<SeenByRoom>(loadSeen());

//------------------ FUNCTIONS -------------------------------
/** Ajoute un constat, sauf s'il en existe un plus récent ; replaces = numéro corrigé, dont le constat est retiré. */
function withSeen(map: SeenByRoom, roomId: number, serialNumber: string, entry: Seen, replaces?: string): SeenByRoom {
    const inRoom = {...map[String(roomId)]};
    if (replaces !== undefined) delete inRoom[serialKey(replaces)];
    const key = serialKey(serialNumber);
    const existing = inRoom[key];
    if (!existing || Date.parse(entry.at) >= Date.parse(existing.at)) inRoom[key] = entry;
    return {...map, [String(roomId)]: inRoom};
}

function withoutRoom(map: SeenByRoom, roomId: number): SeenByRoom {
    const others = {...map};
    delete others[String(roomId)];
    return others;
}

/**
 * Relit les constats enregistrés. Ceux d'une version précédente (date seule, jamais envoyés au serveur)
 * sont convertis et mis en file pour être partagés.
 */
function loadSeen(): SeenByRoom {
    const stored = readStored<Record<string, Record<string, Seen | string>>>(SEEN_KEY, {});
    let map: SeenByRoom = {};
    for (const [roomId, inRoom] of Object.entries(stored)) {
        for (const [serialNumber, value] of Object.entries(inRoom)) {
            if (typeof value !== 'string') {
                map = withSeen(map, Number(roomId), serialNumber, value);
                continue;
            }
            map = withSeen(map, Number(roomId), serialNumber, {at: value, by: null});
            enqueue({kind: 'sight', serialNumber, roomId: Number(roomId), materialId: 0, request: null, seenAt: value});
        }
    }
    writeStored(SEEN_KEY, map);
    return map;
}

function save(): void {
    writeStored(SEEN_KEY, seen.value);
}

/**
 * Note qu'un numéro a été scanné dans une salle, pour tous les agents.
 * @param roomId salle où il a été scanné
 * @param serialNumber numéro retenu
 * @param replaces numéro lu, remplacé par serialNumber (corrigé par l'agent ou trouvé sur le serveur) : oublié
 */
function markSeen(roomId: number, serialNumber: string, replaces?: string): void {
    const at = new Date().toISOString();
    seen.value = withSeen(seen.value, roomId, serialNumber, {at, by: currentUser.value?.username ?? null}, replaces);
    save();
    enqueue({kind: 'sight', serialNumber, roomId, materialId: 0, request: null, seenAt: at, replaces});
    void flush();
}

/** Recommence la reconnaissance d'une salle : ses constats sont effacés pour tous (MANAGER, ADMIN). */
function resetRoom(roomId: number): void {
    seen.value = withoutRoom(seen.value, roomId);
    save();
    enqueue({kind: 'clear-room', serialNumber: '', roomId, materialId: 0, request: null});
    void flush();
}

/**
 * Recharge les constats du serveur, puis réapplique, dans l'ordre, ceux encore en file.
 * @throws ApiError si l'API répond en erreur ou ne répond pas
 */
async function download(): Promise<void> {
    let map: SeenByRoom = {};
    for (const sighting of await sightingsServices.findAll()) {
        map = withSeen(map, sighting.roomId, sighting.serialNumber, {at: sighting.seenAt, by: sighting.seenBy});
    }
    for (const action of readyActions.value) {
        if (action.roomId === null) continue;
        if (action.kind === 'sight') {
            map = withSeen(map, action.roomId, action.serialNumber,
                {at: action.seenAt ?? action.createdAt, by: currentUser.value?.username ?? null}, action.replaces);
        }
        if (action.kind === 'clear-room') map = withoutRoom(map, action.roomId);
    }
    seen.value = map;
    save();
}

/** Constats d'une salle (lecture seule). */
function seenInRoom(roomId: number): Readonly<Record<string, Seen>> {
    return seen.value[String(roomId)] ?? {};
}

/**
 * Accès aux constats partagés.
 * @returns seenInRoom(), markSeen(), resetRoom(), download()
 */
export function useSightings() {
    return {seenInRoom, markSeen, resetRoom, download};
}

export type {Seen};
