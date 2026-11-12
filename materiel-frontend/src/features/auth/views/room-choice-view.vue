<script setup lang="ts">
/**
 * Choix de la salle de cours, obligatoire pour le formateur juste après la connexion (router/guards.ts).
 * Page centrée comme la connexion, sans menu (App.vue) : les salles en tuiles, groupées par bâtiment ;
 * un clic sur une salle la choisit. Ensuite : retour à la page demandée (?redirect=), avec la salle
 * dans l'adresse (?salle=), que la garde recopie de page en page. On change ensuite de salle avec le
 * bouton de la barre du haut (teaching-room-switch.vue).
 * Utilisé par : router/routes.ts (route room-choice)
 */
import {computed, onMounted, ref} from 'vue';
import type {Room} from '../../../models/room.model.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useLocations} from '../../../shared/composables/use-locations.ts';
import {byBuildingThenNumber} from '../../../shared/composables/use-room-label.ts';
import {TEACHING_ROOM_QUERY, useTeachingRoom} from '../../../shared/composables/use-teaching-room.ts';
import {navigateAfterLogin, navigateTo} from '../../../router/navigation.ts';
import LoadState from '../../../shared/components/load-state.vue';

/** Salles d'un même bâtiment. */
interface BuildingGroup {
  id: number;
  number: number;
  rooms: Room[];
}

//------------------ DATAS -----------------------------------
const {currentUser, logout} = useAuth();
const {loading, errorMessage, run} = useAsyncState();
const {rooms, loadLocations} = useLocations();
const {teachingRoomId, needsRoomChoice} = useTeachingRoom();
/** Vrai dès qu'une salle est choisie, pour éviter un double clic pendant la navigation. */
const choosing = ref(false);

//------------------ COMPUTED --------------------------------
const greeting = computed(() => `Bonjour ${currentUser.value?.username ?? ''}`);
const showEmpty = computed(() => !loading.value && errorMessage.value === null && rooms.value.length === 0);
/** Salles triées par bâtiment puis par numéro, regroupées par bâtiment. */
const groups = computed<BuildingGroup[]>(() => [...rooms.value].sort(byBuildingThenNumber).reduce<BuildingGroup[]>((list, room) => {
  const last = list.at(-1);
  if (last?.id === room.building.id) last.rooms.push(room);
  else list.push({id: room.building.id, number: room.building.number, rooms: [room]});
  return list;
}, []));

//------------------ FUNCTIONS -------------------------------
/** Charge les salles proposées. */
async function load(): Promise<void> {
  await run(loadLocations);
}

/** Vrai pour la salle actuelle (quand on revient changer de salle). */
const isCurrent = (room: Room): boolean => room.id === teachingRoomId.value;

/**
 * Choisit la salle, puis ouvre la page demandée avant la connexion (ou le tableau de bord).
 * @param room salle choisie
 */
async function choose(room: Room): Promise<void> {
  choosing.value = true;
  await navigateAfterLogin({[TEACHING_ROOM_QUERY]: String(room.id)});
}

/** Retour à la connexion, pour se connecter avec un autre compte. */
async function signOut(): Promise<void> {
  await logout();
  await navigateTo({name: 'login'});
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
</script>

<template>
  <div class="room-choice">
    <div class="login-brand">
      <span class="brand-logo" aria-hidden="true">B</span>
      <h1>{{ greeting }}</h1>
      <p>Dans quelle salle faites-vous cours ?</p>
    </div>
    <LoadState
        :loading="loading"
        :error-message="errorMessage"
        @retry="load"
    />
    <p v-if="showEmpty" class="empty-state">Aucune salle n'est encore créée : demandez à l'administrateur d'ajouter la vôtre.</p>
    <div v-if="groups.length > 0" class="card room-picker">
      <section v-for="group in groups" :key="group.id">
        <h2 class="room-group-title">Bâtiment {{ group.number }}</h2>
        <ul class="room-tiles">
          <li v-for="room in group.rooms" :key="room.id">
            <button
                type="button"
                class="room-tile"
                :class="{ current: isCurrent(room) }"
                :disabled="choosing"
                @click="choose(room)"
            >
              <span class="room-tile-label">Salle</span>
              <strong class="room-tile-number">{{ room.number }}</strong>
            </button>
          </li>
        </ul>
      </section>
    </div>
    <p v-if="needsRoomChoice" class="room-choice-footer">
      <button
          type="button"
          class="text-button"
          @click="signOut"
      >
        Se connecter avec un autre compte
      </button>
    </p>
  </div>
</template>
