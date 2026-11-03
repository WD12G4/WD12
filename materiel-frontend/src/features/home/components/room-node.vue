<script setup lang="ts">
/**
 * Nœud « salle » de l'arbre : dépliable, avec le nombre de matériels.
 * <details>/<summary> est accessible au clavier sans code supplémentaire.
 * Utilisé par : building-node.vue
 */
import type {RoomNode} from '../composables/use-inventory-tree.ts';
import MaterialLeafList from './material-leaf-list.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Salle et son matériel. */
  node: RoomNode;
}>();
</script>

<template>
  <details>
    <summary>
      Salle {{ node.room.number }}
      <span class="count">({{ node.materials.length }} matériel(s))</span>
    </summary>
    <div class="tree-children">
      <MaterialLeafList :materials="node.materials" />
    </div>
  </details>
</template>
