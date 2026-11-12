/**
 * Salles de la supervision et salle affichée.
 * Formateur : sa salle de cours, celle de l'adresse (?salle=, use-teaching-room.ts), sans autre choix ici ;
 * une salle inconnue (adresse modifiée à la main, salle supprimée) renvoie au choix de la salle.
 * Gestionnaire et administrateur : toutes les salles, la première (par bâtiment, puis par numéro) d'abord.
 * Utilisé par : supervision-view.vue
 */
import {computed, ref} from 'vue';
import {replaceWith} from '../../../router/navigation.ts';
import {useLocations} from '../../../shared/composables/use-locations.ts';
import {byBuildingThenNumber} from '../../../shared/composables/use-room-label.ts';
import {useTeachingRoom} from '../../../shared/composables/use-teaching-room.ts';

/**
 * Crée la liste des salles et la salle affichée.
 * @returns rooms (vide pour le formateur), selectedRoomId, lockedRoom (salle du formateur) et loadRooms()
 */
export function useSupervisionRooms() {
    //------------------ DATAS -----------------------------------
    const {rooms: allRooms, loadLocations} = useLocations();
    const {isTrainer, teachingRoomId} = useTeachingRoom();
    const selectedRoomId = ref<number | null>(null);

    //------------------ COMPUTED --------------------------------
    /** Salles proposées dans la barre d'outils : aucune pour le formateur, qui n'a pas le choix ici. */
    const rooms = computed(() => (isTrainer.value ? [] : allRooms.value));
    /** Salle de cours du formateur ; null pour les autres rôles. */
    const lockedRoom = computed(() => allRooms.value.find(room => isTrainer.value && room.id === teachingRoomId.value) ?? null);

    //------------------ FUNCTIONS -------------------------------
    /** Charge les salles et choisit la salle affichée, si aucune ne l'est encore (lève ApiError). */
    async function loadRooms(): Promise<void> {
        await loadLocations();
        if (isTrainer.value && !lockedRoom.value) {
            await replaceWith({name: 'room-choice', query: {redirect: '/supervision'}});
            return;
        }
        selectedRoomId.value ??= lockedRoom.value?.id ?? [...allRooms.value].sort(byBuildingThenNumber)[0]?.id ?? null;
    }

    return {rooms, selectedRoomId, lockedRoom, loadRooms};
}
