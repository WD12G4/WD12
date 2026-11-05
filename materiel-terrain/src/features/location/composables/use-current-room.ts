/**
 * Salle où se trouve l'agent : chaque matériel lu y est comparé (conforme, déplacé…).
 * Gardée sur le téléphone : elle survit à un rechargement ou à une mise en veille.
 * Utilisé par : features/location, features/scan
 */
import {computed, ref} from 'vue';
import {readStored, writeStored} from '../../../services/local-store.ts';
import {useInventory} from '../../inventory/composables/use-inventory.ts';

const ROOM_KEY = 'room';

//------------------ DATAS -----------------------------------
const currentRoomId = ref<number | null>(readStored<number | null>(ROOM_KEY, null));
const {roomById} = useInventory();

//------------------ COMPUTED --------------------------------
/** Salle courante, ou null si l'agent n'en a pas choisi (consultation seule). */
const currentRoom = computed(() => (currentRoomId.value === null ? null : roomById(currentRoomId.value)));

//------------------ FUNCTIONS -------------------------------
/**
 * Change de salle courante.
 * @param id identifiant de la salle, ou null pour travailler sans salle
 */
function selectRoom(id: number | null): void {
    currentRoomId.value = id;
    writeStored(ROOM_KEY, id);
}

/**
 * Accès à la salle courante partagée.
 * @returns currentRoom et selectRoom()
 */
export function useCurrentRoom() {
    return {currentRoom, selectRoom};
}
