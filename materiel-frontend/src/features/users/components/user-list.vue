<script setup lang="ts">
/**
 * Tableau des comptes.
 * Composant de présentation.
 * Utilisé par : user-view.vue
 */
import type {User} from '../../../models/user.model.ts';
import UserItem from './user-item.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Comptes à afficher. */
  users: User[];
  /** Identifiant du compte connecté (il ne peut pas se supprimer). */
  currentUserId: number | null;
}>();

const emit = defineEmits<{
  /** Demande de modification d'un compte. */
  edit: [user: User];
  /** Demande de suppression d'un compte. */
  toggleArchive: [user: User];
}>();
</script>

<template>
  <p v-if="users.length === 0" class="empty-state">Aucun compte.</p>
  <table v-else class="data-table">
    <thead>
      <tr>
        <th scope="col">Identifiant</th>
        <th scope="col">Rôle</th>
        <th scope="col">État</th>
        <th scope="col"><span class="visually-hidden">Actions</span></th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="user in users" :key="user.id">
        <UserItem
            :user="user"
            :is-current-user="user.id === currentUserId"
            @edit="emit('edit', $event)"
            @toggle-archive="emit('toggleArchive', $event)"
        />
      </tr>
    </tbody>
  </table>
</template>
