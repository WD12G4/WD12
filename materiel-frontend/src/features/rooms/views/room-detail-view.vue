<script setup lang="ts">
/**
 * Fiche d'une salle : son bâtiment et le matériel qui s'y trouve.
 * Réutilise la liste du matériel de la feature materials (exception tolérée par le cadre, section 3).
 * RG : RG-07, RG-18 (le formateur voit toutes les salles)
 */
import {computed, onMounted, ref, watch} from 'vue';
import type {Room} from '../../../models/room.model.ts';
import {roomsServices} from '../../../services/rooms.services.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {roomLabel} from '../../../shared/composables/use-room-label.ts';
import {useRoomMaterials} from '../composables/use-room-materials.ts';
import {sharedRoomId} from '../../../router/view-transition.ts';
import Card from '../../../shared/components/card.vue';
import Link from '../../../shared/components/Link.vue';
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
const room = ref<Room | null>(null);

//------------------ COMPUTED --------------------------------
const roomId = computed(() => Number(props.id));
const isNotFound = computed(() => errorStatus.value === 404);
const title = computed(() => (room.value ? roomLabel(room.value) : 'Salle'));
/** La carte reçoit le nom de la transition partagée quand on arrive de la tuile (ou qu'on y retourne). */
const heroClass = computed(() => ({'vt-target': roomId.value === sharedRoomId.value}));

//------------------ FUNCTIONS -------------------------------
/** Charge la salle et son matériel en parallèle. */
async function load(): Promise<void> {
  room.value = null;
  await run(async () => {
    const [loadedRoom] = await Promise.all([roomsServices.findById(roomId.value), loadMaterials(roomId.value)]);
    room.value = loadedRoom;
  });
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
watch(() => props.id, load);
</script>

<template>
  <Link class="back-link" :to="{ name: 'rooms' }">← Retour aux salles</Link>

  <Card v-if="isNotFound" title="Salle introuvable">
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
      <MaterialList :materials="materials" :can-archive="false" />
    </template>
  </Card>
</template>
