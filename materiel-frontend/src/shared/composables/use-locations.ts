/**
 * Chargement des bâtiments et des salles, utilisés par plusieurs écrans
 * (filtres du matériel, affectation, arbre du tableau de bord, remontées).
 * Chaque appel crée ses propres listes : rien n'est partagé entre les vues.
 * Utilisé par : les vues des features materials, rooms, home et reports.
 */
import {ref} from 'vue';
import type {Building} from '../../models/building.model.ts';
import type {Room} from '../../models/room.model.ts';
import {buildingsServices} from '../../services/buildings.services.ts';
import {roomsServices} from '../../services/rooms.services.ts';

/**
 * Crée les listes de bâtiments et de salles.
 * @returns buildings, rooms et loadLocations() qui les charge (lève ApiError en cas d'échec)
 */
export function useLocations() {
    //------------------ DATAS -----------------------------------
    const buildings = ref<Building[]>([]);
    const rooms = ref<Room[]>([]);

    //------------------ FUNCTIONS -------------------------------
    /** Charge les deux listes en parallèle : elles sont indépendantes. */
    async function loadLocations(): Promise<void> {
        const [loadedBuildings, loadedRooms] = await Promise.all([
            buildingsServices.findAll(),
            roomsServices.findAll(),
        ]);
        buildings.value = loadedBuildings;
        rooms.value = loadedRooms;
    }

    return {buildings, rooms, loadLocations};
}
