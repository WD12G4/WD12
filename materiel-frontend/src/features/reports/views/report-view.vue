<script setup lang="ts">
/**
 * Liste des remontées pour le gestionnaire et l'administrateur, avec leur document.
 * MAQUETTE : données d'un service simulé (pas d'API backend).
 * RG : RG-22, RG-23
 */
import {onMounted, ref} from 'vue';
import type {Report} from '../../../models/report.model.ts';
import {reportsServices} from '../../../services/reports.services.mock.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import Card from '../../../shared/components/card.vue';
import DemoBanner from '../../../shared/components/demo-banner.vue';
import LoadState from '../../../shared/components/load-state.vue';
import ReportList from '../components/report-list.vue';

//------------------ DATAS -----------------------------------
const {loading, errorMessage, run} = useAsyncState();
const reports = ref<Report[]>([]);

//------------------ FUNCTIONS -------------------------------
/** Charge les remontées. */
async function load(): Promise<void> {
  await run(async () => {
    reports.value = await reportsServices.findAll();
  });
}

/** Marque une remontée comme lue, puis recharge la liste. */
async function markAsRead(report: Report): Promise<void> {
  await run(async () => {
    await reportsServices.markAsRead(report.id);
    reports.value = await reportsServices.findAll();
  });
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
</script>

<template>
  <Card title="Remontées">
    <template #header>
      <DemoBanner />
      <p>Problèmes signalés par les formateurs. Chaque remontée a son document d'état des lieux.</p>
    </template>
    <template #body>
      <LoadState
          :loading="loading"
          :error-message="errorMessage"
          @retry="load"
      />
      <ReportList
          v-if="!loading"
          :reports="reports"
          @mark-as-read="markAsRead"
      />
    </template>
  </Card>
</template>
