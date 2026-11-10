<script setup lang="ts">
/**
 * Liste déroulante de salles, regroupées par bâtiment (intitulés de groupe dans la liste).
 * La valeur liée (v-model) est l'identifiant de la salle, ou null pour l'option vide.
 * Utilisé par : formulaire et affectation du matériel, formulaire de remontée, supervision (plus de 6 salles).
 */
import {computed} from 'vue';
import type {Room} from '../../models/room.model.ts';
import type {SelectOption} from '../composables/use-listbox.ts';
import {byBuildingThenNumber} from '../composables/use-room-label.ts';
import AppSelect from './app-select.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** id de la liste, relié au <label for> du parent. */
  id: string;
  /** Salles proposées. */
  rooms: Room[];
  /** Texte de l'option vide (ex. « En stock »). */
  emptyLabel: string;
  /** Désactive la liste (ex. matériel archivé, RG-06). */
  disabled?: boolean;
}>();

/** Identifiant de la salle choisie, ou null. */
const roomId = defineModel<number | null>({required: true});

//------------------ COMPUTED --------------------------------
/** Option vide, puis les salles triées par bâtiment et par numéro, groupées par bâtiment. */
const options = computed<SelectOption<number | null>[]>(() => [
  {value: null, label: props.emptyLabel},
  ...[...props.rooms]
      .sort(byBuildingThenNumber)
      .map(room => ({value: room.id, label: `Salle ${room.number}`, group: `Bâtiment ${room.building.number}`})),
]);
</script>

<template>
  <AppSelect
      :id="id"
      v-model="roomId"
      :options="options"
      :disabled="disabled"
  />
</template>
