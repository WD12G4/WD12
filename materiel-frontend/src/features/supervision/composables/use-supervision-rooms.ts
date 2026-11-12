/**
 * Salles de la supervision et salle affichée.
 * Formateur : sa salle de cours, choisie à la connexion (use-teaching-room.ts), sans autre choix ici.
 * Gestionnaire et administrateur : toutes les salles, la première (par bâtiment, puis par numéro) d'abord.
 * Utilisé par : supervision-view.vue
 */
import {computed, ref} from 'vue';
import {useLocations} from '../../../shared/composables/use-locations.ts';
import {byBuildingThenNumber} from '../../../shared/composables/use-room-label.ts';
import {useTeachingRoom} from '../../../shared/composables/use-teaching-room.ts';

/**
 * Crée la liste des salles et la salle affichée.
 * @returns rooms, selectedRoomId, lockedRoom (salle imposée au formateur, sinon null) et loadRooms()
 */
export function useSupervisionRooms() {
    //------------------ DATAS -----------------------------------
    const {rooms, loadLocations} = useLocations();
    const {isTrainer, teachingRoom} = useTeachingRoom();
    const selectedRoomId = ref<number | null>(null);

    //------------------ COMPUTED --------------------------------
    /** Salle imposée au formateur ; null pour les autres rôles, qui choisissent dans la barre d'outils. */
    const lockedRoom = computed(() => (isTrainer.value ? teachingRoom.value : null));

    //------------------ FUNCTIONS -------------------------------
    /** Charge les salles et choisit la salle affichée, si aucune ne l'est encore (lève ApiError). */
    async function loadRooms(): Promise<void> {
        await loadLocations();
        selectedRoomId.value ??= lockedRoom.value?.id ?? [...rooms.value].sort(byBuildingThenNumber)[0]?.id ?? null;
    }

    return {rooms, selectedRoomId, lockedRoom, loadRooms};
}
