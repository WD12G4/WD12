<script setup lang="ts">
/**
 * Modification d'un matériel (ADMIN, RG-16).
 */
import {computed, onMounted, ref} from 'vue';
import type {Material, MaterialInput} from '../../../models/material.model.ts';
import {materialsServices} from '../../../services/materials.services.ts';
import {navigateTo} from '../../../router/navigation.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useFlashMessage} from '../../../shared/composables/use-flash-message.ts';
import {useLocations} from '../../../shared/composables/use-locations.ts';
import Card from '../../../shared/components/card.vue';
import LoadState from '../../../shared/components/load-state.vue';
import MaterialForm from '../components/material-form.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Identifiant du matériel, reçu de l'URL (chaîne). */
  id: string;
}>();

//------------------ DATAS -----------------------------------
const {loading, errorMessage, run} = useAsyncState();
const {loading: saving, errorMessage: saveError, run: runSave} = useAsyncState();
const {rooms, loadLocations} = useLocations();
const {show} = useFlashMessage();
const material = ref<Material | null>(null);

//------------------ COMPUTED --------------------------------
const materialId = computed(() => Number(props.id));
const title = computed(() => (material.value ? `Modifier ${material.value.serialNumber}` : 'Modifier le matériel'));

//------------------ FUNCTIONS -------------------------------
/** Charge le matériel à modifier et les salles proposées. */
async function load(): Promise<void> {
  await run(async () => {
    const [loaded] = await Promise.all([materialsServices.findById(materialId.value), loadLocations()]);
    material.value = loaded;
  });
}

/** Enregistre puis revient à la fiche. */
async function save(input: MaterialInput): Promise<void> {
  await runSave(async () => {
    await materialsServices.update(materialId.value, input);
    show('Les modifications ont été enregistrées.', 'success');
    await navigateTo({name: 'material-detail', params: {id: materialId.value}});
  });
}

/** Abandonne la saisie et revient à la fiche. */
async function cancel(): Promise<void> {
  await navigateTo({name: 'material-detail', params: {id: materialId.value}});
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
</script>

<template>
  <Card :title="title">
    <template #body>
      <LoadState
          :loading="loading"
          :error-message="errorMessage"
          @retry="load"
      />
      <MaterialForm
          v-if="material"
          :initial="material"
          :rooms="rooms"
          :submitting="saving"
          :error-message="saveError"
          @submit="save"
          @cancel="cancel"
      />
    </template>
  </Card>
</template>
