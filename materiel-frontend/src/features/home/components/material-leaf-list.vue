<script setup lang="ts">
/**
 * Feuilles de l'arbre : la liste du matériel d'un nœud (salle ou stock).
 * Composant de présentation.
 * Utilisé par : room-node.vue, inventory-tree.vue
 */
import {type Material, materialTypeLabels} from '../../../models/material.model.ts';
import Link from '../../../shared/components/Link.vue';
import MaterialStatusBadge from '../../../shared/components/material-status-badge.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Matériel à lister. */
  materials: Material[];
}>();
</script>

<template>
  <p v-if="materials.length === 0" class="muted">Aucun matériel.</p>
  <ul v-else class="tree-leaves">
    <li v-for="material in materials" :key="material.id">
      <Link :to="{ name: 'material-detail', params: { id: material.id } }">{{ material.serialNumber }}</Link>
      <span class="muted">{{ materialTypeLabels[material.materialType] }}</span>
      <MaterialStatusBadge :material="material" />
    </li>
  </ul>
</template>

<style scoped>
.tree-leaves {
  margin: 0.25rem 0 0.5rem;
  padding: 0;
  list-style: none;
}

.tree-leaves li {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.3rem 0.5rem;
}
</style>
