<script setup lang="ts">
/**
 * Barre d'outils de la supervision : choix de la salle (boutons segmentés s'il y en a peu, liste
 * déroulante sinon, rien s'il n'y en a qu'une) et choix de l'affichage (postes ou journal).
 * Utilisé par : supervision-view.vue
 */
import {computed} from 'vue';
import type {Room} from '../../../models/room.model.ts';
import type {SupervisionTab} from '../composables/use-supervision.ts';
import type {SelectOption} from '../../../shared/composables/use-listbox.ts';
import {byBuildingThenNumber} from '../../../shared/composables/use-room-label.ts';
import RoomSelect from '../../../shared/components/room-select.vue';
import SegmentedControl from '../../../shared/components/segmented-control.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Salles proposées. */
  rooms: Room[];
}>();

/** Salle choisie. */
const roomId = defineModel<number | null>('roomId', {required: true});
/** Affichage choisi. */
const tab = defineModel<SupervisionTab>('tab', {required: true});

//------------------ DATAS -----------------------------------
/** Au-delà, les boutons segmentés prendraient trop de place : liste déroulante. */
const MAX_SEGMENTED_ROOMS = 6;
const tabOptions: SelectOption<SupervisionTab>[] = [
  {value: 'stations', label: 'Postes'},
  {value: 'journal', label: 'Journal'},
];

//------------------ COMPUTED --------------------------------
const showSegmented = computed(() => props.rooms.length > 1 && props.rooms.length <= MAX_SEGMENTED_ROOMS);
const showSelect = computed(() => props.rooms.length > MAX_SEGMENTED_ROOMS);
/** « Salle 10 », ou « Salle 1 · Bât. 201 » si deux bâtiments ont une salle de même numéro. */
const roomOptions = computed<SelectOption<number>[]>(() => [...props.rooms].sort(byBuildingThenNumber).map(room => {
  const shared = props.rooms.some(other => other.id !== room.id && other.number === room.number);
  return {value: room.id, label: shared ? `Salle ${room.number} · Bât. ${room.building.number}` : `Salle ${room.number}`};
}));
/** Valeur des boutons segmentés (une salle est toujours choisie quand ils sont affichés). */
const segmentedRoom = computed({
  get: () => roomId.value ?? 0,
  set: (id: number) => (roomId.value = id),
});
</script>

<template>
  <div class="supervision-toolbar">
    <div v-if="showSegmented" class="field">
      <span id="supervision-room-label" class="field-label">Salle</span>
      <SegmentedControl
          v-model="segmentedRoom"
          :options="roomOptions"
          labelledby="supervision-room-label"
      />
    </div>
    <div v-else-if="showSelect" class="field">
      <label for="supervision-room">Salle</label>
      <RoomSelect
          id="supervision-room"
          v-model="roomId"
          :rooms="rooms"
          empty-label="Choisir une salle"
      />
    </div>
    <div class="field">
      <span id="supervision-tab-label" class="field-label">Affichage</span>
      <SegmentedControl
          v-model="tab"
          :options="tabOptions"
          labelledby="supervision-tab-label"
      />
    </div>
  </div>
</template>
