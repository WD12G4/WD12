<script setup lang="ts">
/**
 * Nœud « bâtiment » de l'arbre : dépliable, avec ses salles.
 * Utilisé par : inventory-tree.vue
 */
import type {BuildingNode} from '../composables/use-inventory-tree.ts';
import RoomNode from './room-node.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Bâtiment, ses salles et son nombre de matériels. */
  node: BuildingNode;
}>();
</script>

<template>
  <details>
    <summary>
      Bâtiment {{ node.building.number }}
      <span class="count">({{ node.rooms.length }} salle(s), {{ node.materialCount }} matériel(s))</span>
    </summary>
    <div class="tree-children">
      <p v-if="node.rooms.length === 0" class="muted">Aucune salle.</p>
      <RoomNode
          v-for="roomNode in node.rooms"
          :key="roomNode.room.id"
          :node="roomNode"
      />
    </div>
  </details>
</template>
