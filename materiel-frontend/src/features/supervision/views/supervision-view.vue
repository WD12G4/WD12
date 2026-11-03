<script setup lang="ts">
/**
 * Supervision d'une salle : le formateur choisit une salle, voit les postes et les processus lancés,
 * et peut arrêter un processus en un clic. L'administrateur peut aussi masteriser un poste.
 * MAQUETTE : données d'un service simulé (pas d'API backend).
 * RG : RG-25, RG-26, RG-28. RG-27 (liste blanche / noire) : pas d'écran, le client se contredit.
 */
import {computed, onMounted, ref, watch} from 'vue';
import type {ProcessInfo} from '../../../models/supervision.model.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useLocations} from '../../../shared/composables/use-locations.ts';
import {type WorkstationView, useSupervision} from '../composables/use-supervision.ts';
import Card from '../../../shared/components/card.vue';
import DemoBanner from '../../../shared/components/demo-banner.vue';
import LoadState from '../../../shared/components/load-state.vue';
import RoomSelect from '../../../shared/components/room-select.vue';
import WorkstationList from '../components/workstation-list.vue';

//------------------ DATAS -----------------------------------
const {hasRole} = useAuth();
const {loading, errorMessage, run} = useAsyncState();
const {rooms, loadLocations} = useLocations();
const {workstationViews, loadRoom, killProcess, reimage} = useSupervision();
const selectedRoomId = ref<number | null>(null);

//------------------ COMPUTED --------------------------------
/** RG-28 : la masterisation est réservée à l'administrateur. */
const canReimage = computed(() => hasRole('ADMIN'));

//------------------ FUNCTIONS -------------------------------
/** Charge la liste des salles. */
async function load(): Promise<void> {
  await run(loadLocations);
}

/** Arrête un processus en un clic, sans confirmation (RG-26 : « en un clic »). */
async function kill(process: ProcessInfo): Promise<void> {
  await run(() => killProcess(process));
}

/** Masterise un poste, après confirmation (opération lourde pour le stagiaire). */
async function reimageWorkstation(view: WorkstationView): Promise<void> {
  if (!window.confirm(`Masteriser ${view.workstation.name} ? Le travail non enregistré sera perdu.`)) return;
  await run(() => reimage(view.workstation));
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
watch(selectedRoomId, roomId => run(() => loadRoom(roomId)));
</script>

<template>
  <Card title="Supervision des postes">
    <template #header>
      <DemoBanner />
      <p>Choisissez une salle pour voir les applications ouvertes sur chaque poste.</p>
    </template>
    <template #body>
      <div class="field">
        <label for="supervision-room">Salle</label>
        <RoomSelect
            id="supervision-room"
            v-model="selectedRoomId"
            :rooms="rooms"
            empty-label="Choisir une salle"
        />
      </div>
      <LoadState
          :loading="loading"
          :error-message="errorMessage"
          @retry="load"
      />
      <WorkstationList
          :views="workstationViews"
          :can-reimage="canReimage"
          :busy="loading"
          @kill="kill"
          @reimage="reimageWorkstation"
      />
    </template>
  </Card>
</template>
