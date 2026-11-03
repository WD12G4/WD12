<script setup lang="ts">
/**
 * Une ligne du tableau des salles.
 * Composant de présentation.
 * Utilisé par : room-list.vue
 */
import type {Room} from '../../../models/room.model.ts';
import Link from '../../../shared/components/Link.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Salle affichée. */
  room: Room;
  /** Vrai si l'utilisateur peut modifier et supprimer (ADMIN, RG-31). */
  canEdit: boolean;
}>();

const emit = defineEmits<{
  /** Demande de modification. */
  edit: [room: Room];
  /** Demande de suppression. */
  remove: [room: Room];
}>();
</script>

<template>
  <td>Bâtiment {{ room.building.number }}</td>
  <td>
    <Link :to="{ name: 'room-detail', params: { id: room.id } }">Salle {{ room.number }}</Link>
  </td>
  <td class="row-actions">
    <template v-if="canEdit">
      <button
          type="button"
          class="secondary-button"
          @click="emit('edit', room)"
      >
        Modifier
      </button>
      <button
          type="button"
          class="delete-button"
          @click="emit('remove', room)"
      >
        Supprimer
      </button>
    </template>
  </td>
</template>
