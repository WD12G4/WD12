<script setup lang="ts">
/**
 * Choix de la salle de cours, obligatoire pour le formateur juste après la connexion (router/guards.ts).
 * La supervision affiche ensuite les postes de cette salle. On y revient pour changer de salle
 * (lien de la salle dans le menu, user-card.vue). Ensuite : retour à la page demandée (?redirect=).
 * Utilisé par : router/routes.ts (route room-choice)
 */
import {computed, onMounted, ref} from 'vue';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useLocations} from '../../../shared/composables/use-locations.ts';
import {useTeachingRoom} from '../../../shared/composables/use-teaching-room.ts';
import {navigateAfterLogin} from '../../../router/navigation.ts';
import Card from '../../../shared/components/card.vue';
import LoadState from '../../../shared/components/load-state.vue';
import RoomSelect from '../../../shared/components/room-select.vue';

//------------------ DATAS -----------------------------------
const {loading, errorMessage, run} = useAsyncState();
const {rooms, loadLocations} = useLocations();
const {teachingRoom, chooseRoom} = useTeachingRoom();
/** Salle sélectionnée dans la liste : la salle actuelle quand on vient en changer. */
const roomId = ref<number | null>(teachingRoom.value?.id ?? null);

//------------------ COMPUTED --------------------------------
const selectedRoom = computed(() => rooms.value.find(room => room.id === roomId.value) ?? null);
const isLoaded = computed(() => !loading.value && errorMessage.value === null);
const hasRooms = computed(() => rooms.value.length > 0);

//------------------ FUNCTIONS -------------------------------
/** Charge les salles proposées. */
async function load(): Promise<void> {
  await run(loadLocations);
}

/** Enregistre la salle puis ouvre la page demandée avant la connexion (ou le tableau de bord). */
async function submit(): Promise<void> {
  if (!selectedRoom.value) return;
  chooseRoom(selectedRoom.value);
  await navigateAfterLogin();
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
</script>

<template>
  <Card title="Votre salle de cours">
    <template #header>
      <p>Choisissez la salle où vous faites cours : la supervision affichera ses postes.</p>
    </template>
    <template #body>
      <LoadState
          :loading="loading"
          :error-message="errorMessage"
          @retry="load"
      />
      <p v-if="isLoaded && !hasRooms" class="empty-state">
        Aucune salle n'est encore créée. Demandez à l'administrateur d'ajouter votre salle.
      </p>
      <form
          v-else-if="isLoaded"
          class="form-grid"
          @submit.prevent="submit"
      >
        <div class="field">
          <label for="teaching-room">Salle</label>
          <RoomSelect
              id="teaching-room"
              v-model="roomId"
              :rooms="rooms"
              empty-label="Choisir une salle"
          />
        </div>
        <div class="full-width">
          <button type="submit" :disabled="!selectedRoom">Continuer</button>
        </div>
      </form>
    </template>
  </Card>
</template>
