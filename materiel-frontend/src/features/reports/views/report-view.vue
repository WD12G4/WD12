<script setup lang="ts">
/**
 * Liste des remontées pour le gestionnaire et l'administrateur, avec leur document ; une remontée
 * est marquée comme traitée (ou rouverte) en un clic.
 * RG : RG-22, RG-23
 */
import {onMounted, ref} from 'vue';
import type {Report} from '../../../models/report.model.ts';
import {reportsServices} from '../../../services/reports.services.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import Card from '../../../shared/components/card.vue';
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

/** Marque une remontée comme traitée, ou la rouvre ; seule la ligne concernée est remplacée. */
async function toggleHandled(report: Report): Promise<void> {
  await run(async () => {
    const updated = await reportsServices.setHandled(report.id, !report.handled);
    reports.value = reports.value.map(item => (item.id === updated.id ? updated : item));
  });
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
</script>

<template>
  <Card title="Remontées">
    <template #header>
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
          @toggle-handled="toggleHandled"
      />
    </template>
  </Card>
</template>
