<script setup lang="ts">
/**
 * Arbre dépliable du parc : bâtiments, puis une branche « En stock » (RG-07).
 * Composant de présentation.
 * Utilisé par : home-view.vue
 */
import type {Material} from '../../../models/material.model.ts';
import type {BuildingNode as BuildingNodeData} from '../composables/use-inventory-tree.ts';
import BuildingNode from './building-node.vue';
import MaterialLeafList from './material-leaf-list.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Bâtiments avec leurs salles. */
  buildings: BuildingNodeData[];
  /** Matériel sans salle (en stock). */
  stockMaterials: Material[];
}>();
</script>

<template>
  <div class="tree">
    <p v-if="buildings.length === 0" class="empty-state">Aucun bâtiment enregistré.</p>
    <BuildingNode
        v-for="node in buildings"
        :key="node.building.id"
        :node="node"
    />

    <!-- RG-07 : le matériel sans salle est en stock -->
    <details>
      <summary>
        En stock
        <span class="count">({{ stockMaterials.length }} matériel(s))</span>
      </summary>
      <div class="tree-children">
        <MaterialLeafList :materials="stockMaterials" />
      </div>
    </details>
  </div>
</template>
