<script setup lang="ts">
/**
 * Tableau des matériels, avec le nombre de résultats et un état vide.
 * Composant de présentation : boucle sur les matériels et relaie les événements de chaque ligne.
 * Utilisé par : material-view.vue, features/rooms/views/room-detail-view.vue
 */
import type {Material} from '../../../models/material.model.ts';
import MaterialItem from './material-item.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Matériels à afficher (déjà filtrés). */
  materials: Material[];
  /** Vrai si l'utilisateur peut archiver (ADMIN, RG-16). */
  canArchive: boolean;
}>();

const emit = defineEmits<{
  /** Demande d'archivage d'un matériel. */
  archive: [material: Material];
}>();
</script>

<template>
  <!-- Nombre de résultats -->
  <p class="list-count">{{ materials.length }} matériel(s)</p>

  <!-- État vide -->
  <p v-if="materials.length === 0" class="empty-state">
    Aucun matériel à afficher. Modifiez la recherche ou les filtres.
  </p>

  <!-- Tableau -->
  <table v-else class="data-table">
    <thead>
      <tr>
        <th scope="col">Numéro de série</th>
        <th scope="col">Type</th>
        <th scope="col">Emplacement</th>
        <th scope="col">Fin de disponibilité</th>
        <th scope="col">Statut</th>
        <th scope="col"><span class="visually-hidden">Actions</span></th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="material in materials" :key="material.id">
        <MaterialItem
            :material="material"
            :can-archive="canArchive"
            @archive="emit('archive', $event)"
        />
      </tr>
    </tbody>
  </table>
</template>
