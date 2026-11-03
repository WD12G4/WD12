<script setup lang="ts">
/**
 * Tableau des bâtiments, avec le nombre de salles de chacun.
 * Composant de présentation.
 * Utilisé par : building-view.vue
 */
import type {Building} from '../../../models/building.model.ts';
import BuildingItem from './building-item.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Bâtiments à afficher. */
  buildings: Building[];
  /** Nombre de salles par identifiant de bâtiment. */
  roomCounts: ReadonlyMap<number, number>;
  /** Vrai si l'utilisateur peut modifier et supprimer (ADMIN). */
  canEdit: boolean;
}>();

const emit = defineEmits<{
  /** Demande de modification d'un bâtiment. */
  edit: [building: Building];
  /** Demande de suppression d'un bâtiment. */
  remove: [building: Building];
}>();
</script>

<template>
  <p v-if="buildings.length === 0" class="empty-state">Aucun bâtiment enregistré.</p>
  <table v-else class="data-table">
    <thead>
      <tr>
        <th scope="col">Bâtiment</th>
        <th scope="col">Salles</th>
        <th scope="col"><span class="visually-hidden">Actions</span></th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="building in buildings" :key="building.id">
        <BuildingItem
            :building="building"
            :room-count="roomCounts.get(building.id) ?? 0"
            :can-edit="canEdit"
            @edit="emit('edit', $event)"
            @remove="emit('remove', $event)"
        />
      </tr>
    </tbody>
  </table>
</template>
