<script setup lang="ts">
/**
 * Signalement d'un problème sur un matériel, accessible à tous les rôles.
 * RG : RG-21 (contenu), RG-22 (document créé à la remontée), RG-23 (le gestionnaire est notifié)
 */
import {onMounted, ref} from 'vue';
import type {Material} from '../../../models/material.model.ts';
import type {ReportInput} from '../../../models/report.model.ts';
import {materialsServices} from '../../../services/materials.services.ts';
import {reportsServices} from '../../../services/reports.services.ts';
import {replaceWith} from '../../../router/navigation.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useFlashMessage} from '../../../shared/composables/use-flash-message.ts';
import {useLocations} from '../../../shared/composables/use-locations.ts';
import Card from '../../../shared/components/card.vue';
import LoadState from '../../../shared/components/load-state.vue';
import ReportForm from '../components/report-form.vue';

//------------------ DATAS -----------------------------------
const {loading, errorMessage, run} = useAsyncState();
const {loading: sending, errorMessage: sendError, run: runSend} = useAsyncState();
const {rooms, loadLocations} = useLocations();
const {show} = useFlashMessage();
const materials = ref<Material[]>([]);

//------------------ FUNCTIONS -------------------------------
/** Charge les salles et le matériel proposés dans les listes déroulantes. */
async function load(): Promise<void> {
  await run(async () => {
    const [loaded] = await Promise.all([materialsServices.findAll(), loadLocations()]);
    materials.value = loaded.filter(material => !material.archived);
  });
}

/** Enregistre la remontée puis revient au tableau de bord. */
async function send(input: ReportInput): Promise<void> {
  await runSend(async () => {
    await reportsServices.create(input);
    show('Votre remontée a été transmise au gestionnaire.', 'success');
    await replaceWith({name: 'home'});
  });
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
</script>

<template>
  <Card title="Signaler un problème">
    <template #header>
      <p>Choisissez la salle, le matériel concerné et le type de problème.</p>
    </template>
    <template #body>
      <LoadState
          :loading="loading"
          :error-message="errorMessage"
          @retry="load"
      />
      <ReportForm
          :rooms="rooms"
          :materials="materials"
          :submitting="sending"
          :error-message="sendError"
          @submit="send"
      />
    </template>
  </Card>
</template>
