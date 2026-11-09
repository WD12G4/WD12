<script setup lang="ts">
/**
 * Champs de recherche et de filtre de la liste des matériels (texte, type, archivés).
 * Les filtres d'emplacement sont dans material-location-filters.vue.
 * L'objet de filtres est lié en v-model (comme todo-filters.vue du cours).
 * Utilisé par : features/materials/views/material-view.vue
 */
import type {Building} from '../../../models/building.model.ts';
import type {Room} from '../../../models/room.model.ts';
import {
  type MaterialFilters, type MaterialTypeFilter, materialTypeLabels, materialTypes,
} from '../../../models/material.model.ts';
import type {SelectOption} from '../../../shared/composables/use-listbox.ts';
import AppSelect from '../../../shared/components/app-select.vue';
import MaterialLocationFilters from './material-location-filters.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Bâtiments proposés dans le filtre. */
  buildings: Building[];
  /** Toutes les salles, transmises aux filtres d'emplacement. */
  rooms: Room[];
}>();

/** Critères de recherche, modifiés directement par les champs. */
const filters = defineModel<MaterialFilters>('filters', {required: true});

//------------------ DATAS -----------------------------------
/** « Tous les types », puis chaque type par son libellé. */
const typeOptions: SelectOption<MaterialTypeFilter>[] = [
  {value: 'all', label: 'Tous les types'},
  ...materialTypes.map(type => ({value: type, label: materialTypeLabels[type]})),
];
</script>

<template>
  <div class="filters">
    <div class="field search-field">
      <label for="material-search">Rechercher</label>
      <input
          id="material-search"
          v-model="filters.search"
          type="search"
          placeholder="Numéro de série, salle ou bâtiment…"
      >
    </div>
    <div class="field">
      <label for="material-type-filter">Type</label>
      <AppSelect
          id="material-type-filter"
          v-model="filters.type"
          :options="typeOptions"
      />
    </div>
    <MaterialLocationFilters
        v-model:filters="filters"
        :buildings="buildings"
        :rooms="rooms"
    />
    <label class="checkbox-field">
      <input v-model="filters.showArchived" type="checkbox">
      Afficher les archivés
    </label>
  </div>
</template>
