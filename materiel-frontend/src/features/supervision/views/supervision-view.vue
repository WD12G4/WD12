<script setup lang="ts">
/**
 * Supervision d'une salle : les postes en tuiles (état, applications ouvertes), le détail d'un poste
 * dans le panneau latéral (où l'on ferme une application en un clic), un résumé de la salle et le
 * journal des actions. L'administrateur peut aussi masteriser un poste.
 * Le formateur voit sa salle de cours, choisie à la connexion ; les autres rôles choisissent la salle.
 * Données de l'agent de supervision (API /supervision), relues toutes les 10 s ; masterisation simulée.
 * RG : RG-25, RG-26, RG-28 ; journal (entretien 4). RG-27 (liste noire) : bonus, pas encore d'écran.
 */
import {computed, onMounted, ref, watch} from 'vue';
import type {ProcessInfo} from '../../../models/supervision.model.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {type SupervisionTab, type WorkstationView, useSupervision} from '../composables/use-supervision.ts';
import {useSupervisionRooms} from '../composables/use-supervision-rooms.ts';
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
const {rooms, selectedRoomId, lockedRoom, loadRooms} = useSupervisionRooms();
const {workstationViews, events, loadRoom, killProcess, killEverywhere, reimage} = useSupervision();
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
/** Charge les salles et choisit la salle affichée (use-supervision-rooms.ts). */
async function load(): Promise<void> {
  await run(loadRooms);
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
      <DemoBanner>Postes, applications et arrêts réels (agent de supervision) ; seule la masterisation est simulée.</DemoBanner>
      <p>Cliquez sur un poste pour voir ses applications et en fermer une.</p>
    </template>
    <template #body>
      <SupervisionToolbar
          v-model:room-id="selectedRoomId"
          v-model:tab="tab"
          :rooms="rooms"
          :locked-room="lockedRoom"
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
            @open="openedId = $event"
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
