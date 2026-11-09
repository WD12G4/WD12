<script setup lang="ts">
/**
 * Filtres d'emplacement de la liste des matériels : bâtiment, salle, affecté ou en stock.
 * Partage l'objet de filtres de material-filters.vue (v-model).
 * Utilisé par : material-filters.vue
 * RG : RG-07 (en stock = sans salle)
 */
import {computed} from 'vue';
import type {Building} from '../../../models/building.model.ts';
import type {MaterialFilters, MaterialLocationFilter} from '../../../models/material.model.ts';
import type {Room} from '../../../models/room.model.ts';
import type {SelectOption} from '../../../shared/composables/use-listbox.ts';
import AppSelect from '../../../shared/components/app-select.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Bâtiments proposés. */
  buildings: Building[];
  /** Toutes les salles ; seules celles du bâtiment choisi sont proposées. */
  rooms: Room[];
}>();

/** Critères de recherche, partagés avec material-filters.vue. */
const filters = defineModel<MaterialFilters>('filters', {required: true});

//------------------ DATAS -----------------------------------
/** RG-07 : affecté = dans une salle, en stock = sans salle. */
const locationOptions: SelectOption<MaterialLocationFilter>[] = [
  {value: 'all', label: 'Partout'},
  {value: 'assigned', label: 'Affecté à une salle'},
  {value: 'stock', label: 'En stock'},
];

//------------------ COMPUTED --------------------------------
const buildingOptions = computed<SelectOption<number | 'all'>[]>(() => [
  {value: 'all', label: 'Tous les bâtiments'},
  ...props.buildings.map(building => ({value: building.id, label: `Bâtiment ${building.number}`})),
]);

/** Salles du bâtiment sélectionné, ou toutes (groupées par bâtiment) si aucun bâtiment n'est choisi. */
const roomOptions = computed<SelectOption<number | 'all'>[]>(() => {
  const buildingId = filters.value.buildingId;
  const rooms = buildingId === 'all' ? props.rooms : props.rooms.filter(room => room.building.id === buildingId);
  return [
    {value: 'all', label: 'Toutes les salles'},
    ...rooms.map(room => ({value: room.id, label: `Salle ${room.number}`, group: `Bâtiment ${room.building.number}`})),
  ];
});
</script>

<template>
  <div class="field">
    <label for="material-building-filter">Bâtiment</label>
    <AppSelect
        id="material-building-filter"
        v-model="filters.buildingId"
        :options="buildingOptions"
    />
  </div>
  <div class="field">
    <label for="material-room-filter">Salle</label>
    <AppSelect
        id="material-room-filter"
        v-model="filters.roomId"
        :options="roomOptions"
    />
  </div>
  <div class="field">
    <label for="material-location-filter">Emplacement</label>
    <AppSelect
        id="material-location-filter"
        v-model="filters.location"
        :options="locationOptions"
    />
  </div>
</template>
