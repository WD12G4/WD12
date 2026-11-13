<script setup lang="ts">
/**
 * Supervision d'une salle : les postes en tuiles (état, applications ouvertes), le détail d'un poste
 * dans le panneau latéral (où l'on ferme une application en un clic), un résumé de la salle et le
 * journal des actions. Des postes simulés (marqués « Simulé ») s'ajoutent aux vrais postes ;
 * l'administrateur les masterise avec un assistant (système, image, applications).
 * Le formateur voit sa salle de cours, choisie à la connexion ; les autres rôles choisissent la salle.
 * Vrais postes : agent de supervision (API /supervision), relus toutes les 10 s. Postes simulés : maquette.
 * RG : RG-25, RG-26, RG-27 (liste noire, administrateur), RG-28 ; journal (entretien 4).
 */
import {computed, onMounted, ref, watch} from 'vue';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {type SupervisionTab, useSupervision} from '../composables/use-supervision.ts';
import {useSupervisionActions} from '../composables/use-supervision-actions.ts';
import {useSupervisionRooms} from '../composables/use-supervision-rooms.ts';
import BlacklistPanel from '../components/blacklist-panel.vue';
import Card from '../../../shared/components/card.vue';
import DemoBanner from '../../../shared/components/demo-banner.vue';
import FormError from '../../../shared/components/form-error.vue';
import LoadState from '../../../shared/components/load-state.vue';
import ReimageWizard from '../components/reimage-wizard.vue';
import RoomSummary from '../components/room-summary.vue';
import StationGrid from '../components/station-grid.vue';
import SupervisionJournal from '../components/supervision-journal.vue';
import SupervisionToolbar from '../components/supervision-toolbar.vue';
import WorkstationPanel from '../components/workstation-panel.vue';

//------------------ DATAS -----------------------------------
const {hasRole} = useAuth();
/** Chargements (salles, postes) : indicateur visible. Actions : boutons désactivés (use-supervision-actions.ts). */
const {loading, errorMessage, run} = useAsyncState();
const {rooms, selectedRoomId, lockedRoom, loadRooms} = useSupervisionRooms();
const supervision = useSupervision();
const {workstationViews, events, loadRoom} = supervision;
const {busy, actionError, wizardPreselect, simulatedViews, kill, forbid, closeEverywhere, openWizard, launchReimage} = useSupervisionActions(supervision);
const tab = ref<SupervisionTab>('stations');
const openedId = ref<number | null>(null);

//------------------ COMPUTED --------------------------------
/** RG-28 et RG-27 : la masterisation et la liste noire sont réservées à l'administrateur. */
const canReimage = computed(() => hasRole('ADMIN'));
/** Les tuiles, une fois la salle chargée (pas de « Aucun poste » pendant le chargement). */
const showStations = computed(() => tab.value === 'stations' && (workstationViews.value.length > 0 || !loading.value));
const showJournal = computed(() => tab.value === 'journal');
const showBlacklist = computed(() => canReimage.value && tab.value === 'blacklist');
const openedView = computed(() => workstationViews.value.find(view => view.workstation.id === openedId.value) ?? null);

//------------------ FUNCTIONS -------------------------------
/** Charge les salles et choisit la salle affichée (use-supervision-rooms.ts). */
async function load(): Promise<void> {
  await run(loadRooms);
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
      <DemoBanner>Postes réels (agent de supervision) et postes simulés, marqués « Simulé » : la masterisation n'est possible que sur ces derniers.</DemoBanner>
      <p>Cliquez sur un poste pour voir ses applications et en fermer une.</p>
    </template>
    <template #body>
      <SupervisionToolbar
          v-model:room-id="selectedRoomId"
          v-model:tab="tab"
          :rooms="rooms"
          :locked-room="lockedRoom"
          :can-manage-blacklist="canReimage"
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
            :can-reimage="canReimage"
            @kill-everywhere="closeEverywhere"
            @reimage="openWizard()"
        />
        <StationGrid
            :views="workstationViews"
            :selected-id="openedId"
            @open="openedId = $event"
        />
      </template>
      <SupervisionJournal v-if="showJournal" :events="events" />
      <BlacklistPanel v-if="showBlacklist" />
    </template>
  </Card>
  <WorkstationPanel
      :view="openedView"
      :can-reimage="canReimage"
      :busy="busy"
      @close="openedId = null"
      @kill="kill"
      @forbid="forbid"
      @reimage="openWizard"
  />
  <ReimageWizard
      v-if="wizardPreselect"
      :stations="simulatedViews"
      :preselected="wizardPreselect"
      @close="wizardPreselect = null"
      @launch="launchReimage"
  />
</template>
