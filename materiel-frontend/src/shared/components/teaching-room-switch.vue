<script setup lang="ts">
/**
 * Bouton de la salle de cours du formateur, dans la barre du haut (à droite de « Signaler un
 * problème ») : affiche « Salle 10 » et ouvre la liste des salles pour en changer en un clic.
 * La salle choisie remplace ?salle= dans l'adresse de la page courante (use-teaching-room.ts).
 * Utilisé par : app-nav.vue (formateur uniquement)
 */
import {computed, onMounted, ref, watch} from 'vue';
import type {SelectOption} from '../composables/use-listbox.ts';
import {useLocations} from '../composables/use-locations.ts';
import {byBuildingThenNumber} from '../composables/use-room-label.ts';
import {TEACHING_ROOM_QUERY, useTeachingRoom} from '../composables/use-teaching-room.ts';
import {replaceQuery} from '../../router/navigation.ts';
import AppIcon from './app-icon.vue';
import AppSelect from './app-select.vue';

//------------------ DATAS -----------------------------------
const {rooms, loadLocations} = useLocations();
const {teachingRoomId} = useTeachingRoom();
const selected = ref<number | null>(teachingRoomId.value);

//------------------ COMPUTED --------------------------------
/** Salles groupées par bâtiment, triées par numéro. */
const options = computed<SelectOption<number | null>[]>(() => [...rooms.value]
    .sort(byBuildingThenNumber)
    .map(room => ({value: room.id, label: `Salle ${room.number}`, group: `Bâtiment ${room.building.number}`})));
const currentRoom = computed(() => rooms.value.find(room => room.id === teachingRoomId.value) ?? null);
const label = computed(() => (currentRoom.value ? `Salle ${currentRoom.value.number}` : 'Salle'));

//------------------ FUNCTIONS -------------------------------
/** Charge les salles ; en cas d'échec, le bouton affiche seulement « Salle ». */
async function load(): Promise<void> {
  try {
    await loadLocations();
  } catch {
    rooms.value = [];
  }
}

/**
 * Change de salle : la page courante est rouverte avec la nouvelle salle dans l'adresse.
 * @param roomId salle choisie dans la liste
 */
function onSelect(roomId: number | null): void {
  if (roomId === null || roomId === teachingRoomId.value) return;
  void replaceQuery({[TEACHING_ROOM_QUERY]: String(roomId)});
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
watch(selected, onSelect);
watch(teachingRoomId, roomId => (selected.value = roomId));
</script>

<template>
  <div class="room-switch">
    <label for="teaching-room-switch" class="visually-hidden">Changer de salle de cours</label>
    <AppSelect
        id="teaching-room-switch"
        v-model="selected"
        :options="options"
    >
      <template #trigger>
        <AppIcon name="room" />
        <span>{{ label }}</span>
      </template>
    </AppSelect>
  </div>
</template>
