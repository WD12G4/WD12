<script setup lang="ts">
/**
 * Liste des bâtiments ; création, modification et suppression pour l'administrateur.
 * RG : RG-12, RG-31
 */
import {computed, onMounted, ref} from 'vue';
import type {Building, BuildingInput} from '../../../models/building.model.ts';
import {buildingsServices} from '../../../services/buildings.services.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useFlashMessage} from '../../../shared/composables/use-flash-message.ts';
import {useLocations} from '../../../shared/composables/use-locations.ts';
import {useRoomCounts} from '../composables/use-room-counts.ts';
import Card from '../../../shared/components/card.vue';
import LoadState from '../../../shared/components/load-state.vue';
import BuildingForm from '../components/building-form.vue';
import BuildingList from '../components/building-list.vue';

//------------------ DATAS -----------------------------------
const {hasRole} = useAuth();
const {show} = useFlashMessage();
const {loading, errorMessage, run} = useAsyncState();
const {loading: saving, errorMessage: saveError, run: runSave} = useAsyncState();
const {buildings, rooms, loadLocations} = useLocations();
const {roomCounts} = useRoomCounts(rooms);
/** Bâtiment en cours de modification, ou null en mode création. */
const editing = ref<Building | null>(null);
/** Changer cette clé recrée le formulaire, donc le vide (après un succès). */
const formKey = ref(0);

//------------------ COMPUTED --------------------------------
/** RG-31 : seul l'administrateur gère les bâtiments. */
const canEdit = computed(() => hasRole('ADMIN'));
const formTitle = computed(() => {
  const building = editing.value;
  return building ? `Modifier le bâtiment ${building.number}` : 'Nouveau bâtiment';
});

//------------------ FUNCTIONS -------------------------------
/** Charge les bâtiments et les salles (pour compter les salles de chaque bâtiment). */
async function load(): Promise<void> {
  await run(loadLocations);
}

/** Crée ou modifie selon le mode, puis recharge la liste. */
async function save(input: BuildingInput): Promise<void> {
  const succeeded = await runSave(async () => {
    if (editing.value) await buildingsServices.update(editing.value.id, input);
    else await buildingsServices.create(input);
  });
  if (succeeded) {
    show('Le bâtiment a été enregistré.', 'success');
    editing.value = null;
    formKey.value++;
    await load();
  }
}

/** Supprime après confirmation. TODO(question): que deviennent les salles du bâtiment (RG-31) ? */
async function remove(building: Building): Promise<void> {
  const confirmed = window.confirm(
    `Supprimer le bâtiment ${building.number} ? Ses salles et leur matériel pourraient être affectés.`,
  );
  if (!confirmed) return;
  await run(async () => {
    await buildingsServices.delete(building.id);
    await loadLocations();
  });
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
</script>

<template>
  <Card v-if="canEdit" :title="formTitle">
    <template #body>
      <BuildingForm
          :key="formKey"
          :initial="editing"
          :submitting="saving"
          :error-message="saveError"
          @submit="save"
          @cancel="editing = null"
      />
    </template>
  </Card>
  <Card title="Bâtiments">
    <template #body>
      <LoadState
          :loading="loading"
          :error-message="errorMessage"
          @retry="load"
      />
      <BuildingList
          v-if="!loading"
          :buildings="buildings"
          :room-counts="roomCounts"
          :can-edit="canEdit"
          @edit="editing = $event"
          @remove="remove"
      />
    </template>
  </Card>
</template>
