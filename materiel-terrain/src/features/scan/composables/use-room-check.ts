/**
 * Bilan de la salle courante : matériels déclarés dans la salle, vus pendant la reconnaissance, manquants.
 * Les numéros vus (recensés ou non) sont les constats partagés entre les téléphones (use-sightings.ts) :
 * un matériel vu par un agent l'est pour tous.
 * Utilisé par : features/scan/views/scan-view.vue, use-scan-result.ts
 */
import {computed, type Ref} from 'vue';
import type {Room} from '../../../models/room.model.ts';
import {useInventory} from '../../inventory/composables/use-inventory.ts';
import {useSightings} from '../../inventory/composables/use-sightings.ts';
import {serialKey} from '../lib/identify.ts';

//------------------ DATAS -----------------------------------
const {materials} = useInventory();
const {seenInRoom, markSeen, resetRoom} = useSightings();

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
