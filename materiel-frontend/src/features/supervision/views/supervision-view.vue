<script setup lang="ts">
/**
 * Supervision d'une salle : les postes en tuiles (état, stagiaire, applications ouvertes), une
 * application fermée en un clic, le détail d'un poste dans le panneau latéral, un résumé de la salle
 * et le journal des actions. L'administrateur peut aussi masteriser un poste.
 * MAQUETTE : données d'un service simulé (pas d'API backend).
 * RG : RG-25, RG-26, RG-28 ; journal (entretien 4). RG-27 (liste noire) : bonus, pas encore d'écran.
 */
import {computed, onMounted, ref, watch} from 'vue';
import type {ProcessInfo} from '../../../models/supervision.model.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useLocations} from '../../../shared/composables/use-locations.ts';
import {byBuildingThenNumber} from '../../../shared/composables/use-room-label.ts';
import {type SupervisionTab, type WorkstationView, useSupervision} from '../composables/use-supervision.ts';
import Card from '../../../shared/components/card.vue';
import DemoBanner from '../../../shared/components/demo-banner.vue';
import FormError from '../../../shared/components/form-error.vue';
import LoadState from '../../../shared/components/load-state.vue';
import RoomSummary from '../components/room-summary.vue';
import StationGrid from '../components/station-grid.vue';
import SupervisionJournal from '../components/supervision-journal.vue';
import SupervisionToolbar from '../components/supervision-toolbar.vue';
import WorkstationPanel from '../components/workstation-panel.vue';

//------------------ DATAS -----------------------------------
const {hasRole} = useAuth();
/** Chargements (salles, postes) : indicateur visible. Actions : boutons désactivés, sans indicateur. */
const {loading, errorMessage, run} = useAsyncState();
const {loading: busy, errorMessage: actionError, run: act} = useAsyncState();
const {rooms, loadLocations} = useLocations();
const {workstationViews, events, loadRoom, killProcess, killEverywhere, reimage} = useSupervision();
const selectedRoomId = ref<number | null>(null);
const tab = ref<SupervisionTab>('stations');
const openedId = ref<number | null>(null);

//------------------ COMPUTED --------------------------------
/** RG-28 : la masterisation est réservée à l'administrateur. */
const canReimage = computed(() => hasRole('ADMIN'));
/** Les tuiles, une fois la salle chargée (pas de « Aucun poste » pendant le chargement). */
const showStations = computed(() => tab.value === 'stations' && (workstationViews.value.length > 0 || !loading.value));
const showJournal = computed(() => tab.value === 'journal');
const openedView = computed(() => workstationViews.value.find(view => view.workstation.id === openedId.value) ?? null);

//------------------ FUNCTIONS -------------------------------
/** Charge les salles et choisit la première (par bâtiment, puis par numéro). */
async function load(): Promise<void> {
  await run(loadLocations);
  selectedRoomId.value ??= [...rooms.value].sort(byBuildingThenNumber)[0]?.id ?? null;
}

/** Ferme une application en un clic, sans confirmation (RG-26 : « en un clic »). */
async function kill(process: ProcessInfo): Promise<void> {
  await act(() => killProcess(process));
}

/** Ferme une application sur tous les postes de la salle, après confirmation. */
async function closeEverywhere(name: string): Promise<void> {
  if (!window.confirm(`Fermer « ${name} » sur tous les postes de la salle ?`)) return;
  await act(() => killEverywhere(name));
}

/** Masterise un poste, après confirmation (opération lourde pour le stagiaire). */
async function reimageWorkstation(view: WorkstationView): Promise<void> {
  if (!window.confirm(`Masteriser ${view.workstation.name} ? Le travail non enregistré sera perdu.`)) return;
  await act(() => reimage(view.workstation));
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
watch(selectedRoomId, roomId => {
  openedId.value = null;
  void run(() => loadRoom(roomId));
});
</script>

<template>
  <Card class="supervision-card" title="Supervision des postes">
    <template #header>
      <DemoBanner />
      <p>Survolez une application pour la fermer d'un clic ; cliquez sur un poste pour voir son détail.</p>
    </template>
    <template #body>
      <SupervisionToolbar
          v-model:room-id="selectedRoomId"
          v-model:tab="tab"
          :rooms="rooms"
      />
      <LoadState
          :loading="loading"
          :error-message="errorMessage"
          @retry="load"
      />
      <FormError :message="actionError" />
      <template v-if="showStations">
        <RoomSummary
            :views="workstationViews"
            :busy="busy"
            @kill-everywhere="closeEverywhere"
        />
        <StationGrid
            :views="workstationViews"
            :selected-id="openedId"
            :busy="busy"
            @open="openedId = $event"
            @kill="kill"
        />
      </template>
      <SupervisionJournal v-if="showJournal" :events="events" />
    </template>
  </Card>
  <WorkstationPanel
      :view="openedView"
      :can-reimage="canReimage"
      :busy="busy"
      @close="openedId = null"
      @kill="kill"
      @reimage="reimageWorkstation"
  />
</template>
