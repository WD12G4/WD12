<script setup lang="ts">
/**
 * Champs de recherche et de filtre de la liste des matériels (texte, type, archivés).
 * Le type se choisit en un clic, avec des boutons segmentés (types et libellés fournis par l'API).
 * Les filtres d'emplacement sont dans material-location-filters.vue.
 * L'objet de filtres est lié en v-model (comme todo-filters.vue du cours).
 * Utilisé par : features/materials/views/material-view.vue
 */
import {computed} from 'vue';
import type {Building} from '../../../models/building.model.ts';
import type {Room} from '../../../models/room.model.ts';
import type {MaterialFilters, MaterialTypeFilter} from '../../../models/material.model.ts';
import type {SelectOption} from '../../../shared/composables/use-listbox.ts';
import {useMaterialTypes} from '../../../shared/composables/use-material-types.ts';
import SegmentedControl from '../../../shared/components/segmented-control.vue';
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
const {materialTypes} = useMaterialTypes();

//------------------ COMPUTED --------------------------------
/** « Tous », puis chaque type de l'API, dans l'ordre de l'enum du backend. */
const typeOptions = computed<SelectOption<MaterialTypeFilter>[]>(() => [
  {value: 'all', label: 'Tous'},
  ...materialTypes.value.map(type => ({value: type.code, label: type.label})),
]);
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
    <div class="field wide">
      <span id="material-type-label" class="field-label">Type</span>
      <SegmentedControl
          v-model="filters.type"
          :options="typeOptions"
          labelledby="material-type-label"
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
