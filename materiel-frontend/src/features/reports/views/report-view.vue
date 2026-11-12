<script setup lang="ts">
/**
 * Remontées. Gestionnaire et administrateur : toutes les remontées, marquées comme traitées (ou
 * rouvertes) en un clic. Formateur : l'historique de ses propres remontées et leur suivi.
 * Un clic sur une remontée affiche son détail (description complète) dans le panneau latéral ;
 * chaque remontée garde son document d'état des lieux.
 * RG : RG-21, RG-22, RG-23
 */
import {computed, onMounted, ref} from 'vue';
import type {Report} from '../../../models/report.model.ts';
import {reportsServices} from '../../../services/reports.services.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import Card from '../../../shared/components/card.vue';
import FormError from '../../../shared/components/form-error.vue';
import LoadState from '../../../shared/components/load-state.vue';
import ReportList from '../components/report-list.vue';
import ReportPanel from '../components/report-panel.vue';

//------------------ DATAS -----------------------------------
const {currentUser, hasRole} = useAuth();
const {loading, errorMessage, run} = useAsyncState();
/** Traitement d'une remontée : le tableau reste affiché (pas d'indicateur de chargement). */
const {errorMessage: actionError, run: act} = useAsyncState();
const reports = ref<Report[]>([]);
const openedId = ref<number | null>(null);

//------------------ COMPUTED --------------------------------
/** RG-23 : le gestionnaire (et l'administrateur) voit toutes les remontées et les traite. */
const isManager = computed(() => hasRole('MANAGER'));
const title = computed(() => (isManager.value ? 'Remontées' : 'Mes remontées'));
const intro = computed(() => (isManager.value
    ? 'Problèmes signalés par les formateurs. Cliquez sur une remontée pour voir son détail.'
    : 'Les problèmes que vous avez signalés et leur suivi. Cliquez sur une remontée pour voir son détail.'));
const openedReport = computed(() => reports.value.find(report => report.id === openedId.value) ?? null);

//------------------ FUNCTIONS -------------------------------
/** Charge toutes les remontées (gestionnaire) ou celles de l'utilisateur connecté (formateur). */
async function load(): Promise<void> {
  await run(async () => {
    const userId = currentUser.value?.id ?? 0;
    reports.value = isManager.value ? await reportsServices.findAll() : await reportsServices.findByUser(userId);
  });
}

/** Marque une remontée comme traitée, ou la rouvre ; seule la ligne concernée est remplacée. */
async function toggleHandled(report: Report): Promise<void> {
  await act(async () => {
    const updated = await reportsServices.setHandled(report.id, !report.handled);
    reports.value = reports.value.map(item => (item.id === updated.id ? updated : item));
  });
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
</script>

<template>
  <Card class="reports-card" :title="title">
    <template #header>
      <p>{{ intro }}</p>
    </template>
    <template #body>
      <LoadState
          :loading="loading"
          :error-message="errorMessage"
          @retry="load"
      />
      <FormError :message="actionError" />
      <ReportList
          v-if="!loading"
          :reports="reports"
          :selected-id="openedId"
          :show-author="isManager"
          :can-handle="isManager"
          @open="openedId = $event.id"
          @toggle-handled="toggleHandled"
      />
    </template>
  </Card>
  <ReportPanel
      :report="openedReport"
      :show-author="isManager"
      :can-handle="isManager"
      @close="openedId = null"
      @toggle-handled="toggleHandled"
  />
</template>
