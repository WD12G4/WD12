/**
 * Salle de cours du formateur : choisie juste après la connexion, obligatoire pour le formateur.
 * La supervision affiche alors seulement les postes de cette salle (supervision-view.vue).
 * L'état est déclaré au niveau du module, comme use-auth.ts : toutes les pages partagent la même salle.
 * Il reste EN MÉMOIRE (pas de stockage navigateur) : une nouvelle connexion, un changement de compte
 * ou un rechargement de la page redemandent la salle.
 * Utilisé par : router/guards.ts, App.vue, room-choice-view.vue, user-card.vue, supervision-view.vue
 */
import {computed, ref, watch} from 'vue';
import type {Room} from '../../models/room.model.ts';
import {useAuth} from './use-auth.ts';

//------------------ DATAS -----------------------------------
const {currentUser} = useAuth();
const teachingRoom = ref<Room | null>(null);

//------------------ COMPUTED --------------------------------
/** Vrai pour un formateur (rôle USER) : le gestionnaire et l'administrateur ne choisissent pas de salle. */
const isTrainer = computed(() => currentUser.value?.role === 'USER');
/** Vrai tant qu'un formateur connecté n'a pas choisi sa salle : la garde l'envoie sur le choix. */
const needsRoomChoice = computed(() => isTrainer.value && teachingRoom.value === null);

//------------------ FUNCTIONS -------------------------------
/**
 * Enregistre la salle de cours choisie.
 * @param room salle choisie par le formateur
 */
function chooseRoom(room: Room): void {
    teachingRoom.value = room;
}

//------------------ LIFECYCLE -------------------------------
// Connexion, changement de compte, déconnexion ou session restaurée : la salle est redemandée.
// « sync » : remise à zéro immédiate, avant que la garde ne lise needsRoomChoice.
watch(currentUser, () => {
    teachingRoom.value = null;
}, {flush: 'sync'});

/**
 * Accès à la salle de cours partagée.
 * @returns la salle choisie, le rôle formateur, le besoin de choisir et l'action de choix
 */
export function useTeachingRoom() {
    return {teachingRoom, isTrainer, needsRoomChoice, chooseRoom};
}
