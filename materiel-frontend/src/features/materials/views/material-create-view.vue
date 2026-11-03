<script setup lang="ts">
/**
 * Création d'un matériel (ADMIN, RG-16).
 * Enregistrement du matériel arrivant du CND : un numéro de série par matériel (RG-01).
 */
import {onMounted} from 'vue';
import type {MaterialInput} from '../../../models/material.model.ts';
import {materialsServices} from '../../../services/materials.services.ts';
import {navigateTo} from '../../../router/navigation.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useFlashMessage} from '../../../shared/composables/use-flash-message.ts';
import {useLocations} from '../../../shared/composables/use-locations.ts';
import Card from '../../../shared/components/card.vue';
import LoadState from '../../../shared/components/load-state.vue';
import MaterialForm from '../components/material-form.vue';

//------------------ DATAS -----------------------------------
const {loading, errorMessage, run} = useAsyncState();
const {loading: saving, errorMessage: saveError, run: runSave} = useAsyncState();
const {rooms, loadLocations} = useLocations();
const {show} = useFlashMessage();

//------------------ FUNCTIONS -------------------------------
/** Charge les salles proposées dans le formulaire. */
async function load(): Promise<void> {
  await run(loadLocations);
}

/** Crée le matériel puis ouvre sa fiche. */
async function create(input: MaterialInput): Promise<void> {
  await runSave(async () => {
    const created = await materialsServices.create(input);
    show(`Le matériel ${created.serialNumber} a été créé.`, 'success');
    await navigateTo({name: 'material-detail', params: {id: created.id}});
  });
}

/** Abandonne la saisie et revient à la liste. */
async function cancel(): Promise<void> {
  await navigateTo({name: 'materials'});
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
</script>

<template>
  <Card title="Nouveau matériel">
    <template #header>
      <p>Chaque matériel est enregistré avec son propre numéro de série.</p>
    </template>
    <template #body>
      <LoadState
          :loading="loading"
          :error-message="errorMessage"
          @retry="load"
      />
      <MaterialForm
          :initial="null"
          :rooms="rooms"
          :submitting="saving"
          :error-message="saveError"
          @submit="create"
          @cancel="cancel"
      />
    </template>
  </Card>
</template>
