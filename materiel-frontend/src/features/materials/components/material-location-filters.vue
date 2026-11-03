<script setup lang="ts">
/**
 * Filtres d'emplacement de la liste des matériels : bâtiment, salle, affecté ou en stock.
 * Partage l'objet de filtres de material-filters.vue (v-model).
 * Utilisé par : material-filters.vue
 * RG : RG-07 (en stock = sans salle)
 */
import {computed} from 'vue';
import type {Building} from '../../../models/building.model.ts';
import type {MaterialFilters} from '../../../models/material.model.ts';
import type {Room} from '../../../models/room.model.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Bâtiments proposés. */
  buildings: Building[];
  /** Toutes les salles ; seules celles du bâtiment choisi sont proposées. */
  rooms: Room[];
}>();

/** Critères de recherche, partagés avec material-filters.vue. */
const filters = defineModel<MaterialFilters>('filters', {required: true});

//------------------ COMPUTED --------------------------------
/** Salles du bâtiment sélectionné, ou toutes si aucun bâtiment n'est choisi. */
const roomOptions = computed(() => {
  const buildingId = filters.value.buildingId;
  return buildingId === 'all' ? props.rooms : props.rooms.filter(room => room.building.id === buildingId);
});
</script>

<template>
  <div class="field">
    <label for="material-building-filter">Bâtiment</label>
    <select id="material-building-filter" v-model="filters.buildingId">
      <option value="all">Tous les bâtiments</option>
      <option
          v-for="building in buildings"
          :key="building.id"
          :value="building.id"
      >
        Bâtiment {{ building.number }}
      </option>
    </select>
  </div>
  <div class="field">
    <label for="material-room-filter">Salle</label>
    <select id="material-room-filter" v-model="filters.roomId">
      <option value="all">Toutes les salles</option>
      <option
          v-for="room in roomOptions"
          :key="room.id"
          :value="room.id"
      >
        Salle {{ room.number }} (bât. {{ room.building.number }})
      </option>
    </select>
  </div>
  <div class="field">
    <label for="material-location-filter">Emplacement</label>
    <select id="material-location-filter" v-model="filters.location">
      <option value="all">Partout</option>
      <option value="assigned">Affecté à une salle</option>
      <option value="stock">En stock</option>
    </select>
  </div>
</template>
