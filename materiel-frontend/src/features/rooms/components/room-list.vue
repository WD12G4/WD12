<script setup lang="ts">
/**
 * Tableau des salles, triées par bâtiment puis par numéro (ordre renvoyé par l'API).
 * Composant de présentation.
 * Utilisé par : room-view.vue
 */
import type {Room} from '../../../models/room.model.ts';
import RoomItem from './room-item.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Salles à afficher. */
  rooms: Room[];
  /** Vrai si l'utilisateur peut modifier et supprimer (ADMIN). */
  canEdit: boolean;
}>();

const emit = defineEmits<{
  /** Demande de modification d'une salle. */
  edit: [room: Room];
  /** Demande de suppression d'une salle. */
  remove: [room: Room];
}>();
</script>

<template>
  <p v-if="rooms.length === 0" class="empty-state">Aucune salle enregistrée.</p>
  <table v-else class="data-table">
    <thead>
      <tr>
        <th scope="col">Bâtiment</th>
        <th scope="col">Salle</th>
        <th scope="col"><span class="visually-hidden">Actions</span></th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="room in rooms" :key="room.id">
        <RoomItem
            :room="room"
            :can-edit="canEdit"
            @edit="emit('edit', $event)"
            @remove="emit('remove', $event)"
        />
      </tr>
    </tbody>
  </table>
</template>
