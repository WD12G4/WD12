<script setup lang="ts">
/**
 * Une ligne du tableau des bâtiments.
 * Composant de présentation.
 * Utilisé par : building-list.vue
 */
import {computed} from 'vue';
import type {Building} from '../../../models/building.model.ts';
import {countLabel} from '../../../shared/composables/use-plural.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Bâtiment affiché. */
  building: Building;
  /** Nombre de salles du bâtiment. */
  roomCount: number;
  /** Vrai si l'utilisateur peut modifier et supprimer (ADMIN, RG-31). */
  canEdit: boolean;
}>();

const emit = defineEmits<{
  /** Demande de modification. */
  edit: [building: Building];
  /** Demande de suppression. */
  remove: [building: Building];
}>();

//------------------ COMPUTED --------------------------------
const roomCountText = computed(() => countLabel(props.roomCount, 'salle'));
</script>

<template>
  <td>Bâtiment {{ building.number }}</td>
  <td>{{ roomCountText }}</td>
  <td class="row-actions">
    <template v-if="canEdit">
      <button
          type="button"
          class="secondary-button"
          @click="emit('edit', building)"
      >
        Modifier
      </button>
      <button
          type="button"
          class="delete-button"
          @click="emit('remove', building)"
      >
        Supprimer
      </button>
    </template>
  </td>
</template>
