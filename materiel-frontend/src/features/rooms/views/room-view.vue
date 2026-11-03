<script setup lang="ts">
/**
 * Liste des salles ; création, modification et suppression pour l'administrateur.
 * RG : RG-13, RG-31
 */
import {computed, onMounted, ref} from 'vue';
import type {Room, RoomInput} from '../../../models/room.model.ts';
import {roomsServices} from '../../../services/rooms.services.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useFlashMessage} from '../../../shared/composables/use-flash-message.ts';
import {useLocations} from '../../../shared/composables/use-locations.ts';
import Card from '../../../shared/components/card.vue';
import LoadState from '../../../shared/components/load-state.vue';
import RoomForm from '../components/room-form.vue';
import RoomList from '../components/room-list.vue';

//------------------ DATAS -----------------------------------
const {hasRole} = useAuth();
const {show} = useFlashMessage();
const {loading, errorMessage, run} = useAsyncState();
const {loading: saving, errorMessage: saveError, run: runSave} = useAsyncState();
const {buildings, rooms, loadLocations} = useLocations();
/** Salle en cours de modification, ou null en mode création. */
const editing = ref<Room | null>(null);
/** Changer cette clé recrée le formulaire, donc le vide (après un succès). */
const formKey = ref(0);

//------------------ COMPUTED --------------------------------
/** RG-31 : seul l'administrateur gère les salles. */
const canEdit = computed(() => hasRole('ADMIN'));
const formTitle = computed(() => {
  const room = editing.value;
  return room ? `Modifier la salle ${room.number} (bât. ${room.building.number})` : 'Nouvelle salle';
});

//------------------ FUNCTIONS -------------------------------
/** Charge les salles, et les bâtiments proposés dans le formulaire. */
async function load(): Promise<void> {
  await run(loadLocations);
}

/** Crée ou modifie selon le mode, puis recharge la liste. */
async function save(input: RoomInput): Promise<void> {
  const succeeded = await runSave(async () => {
    if (editing.value) await roomsServices.update(editing.value.id, input);
    else await roomsServices.create(input);
  });
  if (succeeded) {
    show('La salle a été enregistrée.', 'success');
    editing.value = null;
    formKey.value++;
    await load();
  }
}

/** Supprime après confirmation. TODO(question): que devient le matériel de la salle (RG-31) ? */
async function remove(room: Room): Promise<void> {
  const confirmed = window.confirm(
    `Supprimer la salle ${room.number} du bâtiment ${room.building.number} ? Le matériel qui s'y trouve pourrait être affecté.`,
  );
  if (!confirmed) return;
  await run(async () => {
    await roomsServices.delete(room.id);
    await loadLocations();
  });
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
</script>

<template>
  <Card v-if="canEdit" :title="formTitle">
    <template #body>
      <RoomForm
          :key="formKey"
          :initial="editing"
          :buildings="buildings"
          :submitting="saving"
          :error-message="saveError"
          @submit="save"
          @cancel="editing = null"
      />
    </template>
  </Card>
  <Card title="Salles">
    <template #body>
      <LoadState
          :loading="loading"
          :error-message="errorMessage"
          @retry="load"
      />
      <RoomList
          v-if="!loading"
          :rooms="rooms"
          :can-edit="canEdit"
          @edit="editing = $event"
          @remove="remove"
      />
    </template>
  </Card>
</template>
