<script setup lang="ts">
/**
 * Document d'état des lieux d'une remontée, imprimable.
 * Le document est affiché par le navigateur ; « Imprimer » permet de l'enregistrer en PDF.
 * TODO(question): le client attend un PDF « créé au moment de la remontée, stocké et récupérable »
 * (entretien 2, 8:58). À générer côté backend ? Décision d'équipe à prendre (cadre, section 12).
 * RG : RG-22
 */
import {computed, onMounted, ref} from 'vue';
import type {Report} from '../../../models/report.model.ts';
import {reportsServices} from '../../../services/reports.services.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import Card from '../../../shared/components/card.vue';
import BackLink from '../../../shared/components/back-link.vue';
import LoadState from '../../../shared/components/load-state.vue';
import ReportDocument from '../components/report-document.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Identifiant de la remontée, reçu de l'URL (chaîne). */
  id: string;
}>();

//------------------ DATAS -----------------------------------
const {loading, errorMessage, errorStatus, run} = useAsyncState();
const report = ref<Report | null>(null);

//------------------ COMPUTED --------------------------------
const reportId = computed(() => Number(props.id));
const isNotFound = computed(() => errorStatus.value === 404);

//------------------ FUNCTIONS -------------------------------
/** Charge la remontée. */
async function load(): Promise<void> {
  await run(async () => {
    report.value = await reportsServices.findById(reportId.value);
  });
}

/** Ouvre la fenêtre d'impression du navigateur (qui propose l'enregistrement en PDF). */
function print(): void {
  window.print();
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
</script>

<template>
  <BackLink class="no-print" :fallback="{ name: 'reports' }" />
  <Card v-if="isNotFound" title="Remontée introuvable">
    <template #body>
      <p>Cette remontée n'existe pas, ou a été supprimée.</p>
    </template>
  </Card>
  <LoadState
      v-else-if="!report"
      :loading="loading"
      :error-message="errorMessage"
      @retry="load"
  />
  <Card v-else title="Document d'état des lieux">
    <template #body>
      <ReportDocument :report="report" />
      <button
          type="button"
          class="no-print"
          @click="print"
      >
        Imprimer / enregistrer en PDF
      </button>
    </template>
  </Card>
</template>
