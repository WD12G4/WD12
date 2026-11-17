<script setup lang="ts">
/**
 * Fiche d'une salle : son bâtiment et le matériel qui s'y trouve.
 * Réutilise la liste du matériel de la feature materials (exception tolérée par le cadre, section 3).
 * Le formateur n'ouvre que la fiche de sa salle de cours : une autre salle lui est refusée ici
 * (filtre du front seulement, l'API ne limite pas encore le formateur à une salle).
 * RG : RG-07, RG-34 (formateur : sa salle seulement)
 */
import {computed, onMounted, ref, watch} from 'vue';
import type {Room} from '../../../models/room.model.ts';
import {roomsServices} from '../../../services/rooms.services.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {roomLabel} from '../../../shared/composables/use-room-label.ts';
import {useTeachingRoom} from '../../../shared/composables/use-teaching-room.ts';
import {useRoomMaterials} from '../composables/use-room-materials.ts';
import {sharedRoomId} from '../../../router/view-transition.ts';
import Card from '../../../shared/components/card.vue';
import BackLink from '../../../shared/components/back-link.vue';
import OutOfRoomCard from '../../../shared/components/out-of-room-card.vue';
import LoadState from '../../../shared/components/load-state.vue';
import MaterialList from '../../materials/components/material-list.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Identifiant de la salle, reçu de l'URL (chaîne). */
  id: string;
}>();

//------------------ DATAS -----------------------------------
const {loading, errorMessage, errorStatus, run} = useAsyncState();
const {materials, loadMaterials} = useRoomMaterials();
const {trainerRoomId} = useTeachingRoom();
const room = ref<Room | null>(null);

//------------------ COMPUTED --------------------------------
const roomId = computed(() => Number(props.id));
const isNotFound = computed(() => errorStatus.value === 404);
/** RG-34 : vrai si un formateur demande une autre salle que sa salle de cours. */
const isOtherRoom = computed(() => trainerRoomId.value !== null && roomId.value !== trainerRoomId.value);
const ownRoom = computed(() => ({name: 'room-detail' as const, params: {id: trainerRoomId.value ?? roomId.value}}));
const title = computed(() => (room.value ? roomLabel(room.value) : 'Salle'));
/** La carte reçoit le nom de la transition partagée quand on arrive de la tuile (ou qu'on y retourne). */
const heroClass = computed(() => ({'vt-target': roomId.value === sharedRoomId.value}));

//------------------ FUNCTIONS -------------------------------
/** Charge la salle et son matériel en parallèle (rien pour une salle refusée au formateur). */
async function load(): Promise<void> {
  room.value = null;
  if (isOtherRoom.value) return;
  await run(async () => {
    const [loadedRoom] = await Promise.all([roomsServices.findById(roomId.value), loadMaterials(roomId.value)]);
    room.value = loadedRoom;
  });
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
watch([() => props.id, trainerRoomId], load);
</script>

<template>
  <!-- Retour à la page d'où l'on vient (tableau de bord ou liste des salles), sinon aux salles -->
  <BackLink :fallback="{ name: 'rooms' }" />

  <OutOfRoomCard
      v-if="isOtherRoom"
      title="Salle non accessible"
      label="Ouvrir ma salle"
      :to="ownRoom"
  />
  <Card v-else-if="isNotFound" title="Salle introuvable">
    <template #body>
      <p>Cette salle n'existe pas ou plus.</p>
    </template>
  </Card>
  <LoadState
      v-else-if="!room"
      :loading="loading"
      :error-message="errorMessage"
      @retry="load"
  />

  <Card
      v-else
      class="room-hero"
      :class="heroClass"
      :title="title"
  >
    <template #header>
      <p>Matériel actuellement affecté à cette salle. L'archivage se fait depuis la fiche du matériel.</p>
    </template>
    <template #body>
      <MaterialList
          :materials="materials"
          :can-manage="false"
          @updated="load"
      />
    </template>
  </Card>
</template>
