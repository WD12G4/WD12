/**
 * Salle de cours du formateur : choisie juste après la connexion, obligatoire pour le formateur.
 * Elle est dans l'adresse de chaque page (?salle=12) : la garde (router/guards.ts) la recopie d'une
 * page à l'autre et la lit ici. Un rechargement ou un lien copié garde donc la salle.
 * La supervision affiche ensuite seulement les postes de cette salle (supervision-view.vue).
 * Utilisé par : router/guards.ts, App.vue, room-choice-view.vue, use-supervision-rooms.ts, use-dev-accounts.ts
 */
import {computed, ref} from 'vue';
import type {LocationQuery} from 'vue-router';
import {useAuth} from './use-auth.ts';

//------------------ DATAS -----------------------------------
/** Nom du paramètre d'adresse qui porte la salle de cours. */
const TEACHING_ROOM_QUERY = 'salle';
const {currentUser} = useAuth();
/** Salle lue dans l'adresse par la garde, à chaque navigation ; null si l'adresse n'en a pas. */
const teachingRoomId = ref<number | null>(null);

//------------------ COMPUTED --------------------------------
/** Vrai pour un formateur (rôle USER) : le gestionnaire et l'administrateur ne choisissent pas de salle. */
const isTrainer = computed(() => currentUser.value?.role === 'USER');
/** Vrai tant qu'un formateur connecté n'a pas de salle : la garde l'envoie sur le choix. */
const needsRoomChoice = computed(() => isTrainer.value && teachingRoomId.value === null);

//------------------ FUNCTIONS -------------------------------
/**
 * Salle de cours écrite dans l'adresse.
 * @param query paramètres d'adresse d'une route
 * @returns l'identifiant de la salle, ou null s'il est absent ou invalide
 */
function roomIdFromQuery(query: LocationQuery): number | null {
    const id = Number(query[TEACHING_ROOM_QUERY]);
    return Number.isInteger(id) && id > 0 ? id : null;
}

/**
 * Enregistre la salle lue dans l'adresse (appelé par la garde).
 * @param id identifiant de la salle, ou null
 */
function setTeachingRoomId(id: number | null): void {
    teachingRoomId.value = id;
}

/**
 * Accès à la salle de cours.
 * @returns la salle de l'adresse, le rôle formateur, le besoin de choisir et la lecture de l'adresse
 */
export function useTeachingRoom() {
    return {teachingRoomId, isTrainer, needsRoomChoice, roomIdFromQuery, setTeachingRoomId};
}

export {TEACHING_ROOM_QUERY};
