/**
 * Nombre de salles par bâtiment, calculé à partir de la liste des salles.
 * Utilisé par : building-view.vue
 */
import {computed, type Ref} from 'vue';
import type {Room} from '../../../models/room.model.ts';

/**
 * Compte les salles de chaque bâtiment.
 * @param rooms toutes les salles
 * @returns un computed : identifiant de bâtiment → nombre de salles
 */
export function useRoomCounts(rooms: Ref<Room[]>) {
    const roomCounts = computed(() => {
        const counts = new Map<number, number>();
        for (const room of rooms.value) {
            counts.set(room.building.id, (counts.get(room.building.id) ?? 0) + 1);
        }
        return counts;
    });
    return {roomCounts};
}
