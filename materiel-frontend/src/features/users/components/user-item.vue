<script setup lang="ts">
/**
 * Une ligne du tableau des comptes.
 * Composant de présentation.
 * Utilisé par : user-list.vue
 */
import {computed} from 'vue';
import {roleLabels, type User} from '../../../models/user.model.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Compte affiché. */
  user: User;
  /** Vrai s'il s'agit du compte connecté : il ne peut pas se supprimer lui-même. */
  isCurrentUser: boolean;
}>();

const emit = defineEmits<{
  /** Demande de modification. */
  edit: [user: User];
  /** Demande de suppression. */
  remove: [user: User];
}>();

//------------------ COMPUTED --------------------------------
/** RG-19 : l'état est affiché en texte, pas seulement en couleur. */
const statusLabel = computed(() => (props.user.enabled ? 'Actif' : 'Désactivé'));
const statusClass = computed(() => (props.user.enabled ? 'assigned' : 'archived'));
</script>

<template>
  <td>{{ user.username }}</td>
  <td>{{ roleLabels[user.role] }}</td>
  <td><span class="badge" :class="statusClass">{{ statusLabel }}</span></td>
  <td class="row-actions">
    <button
        type="button"
        class="secondary-button"
        @click="emit('edit', user)"
    >
      Modifier
    </button>
    <button
        v-if="!isCurrentUser"
        type="button"
        class="delete-button"
        @click="emit('remove', user)"
    >
      Supprimer
    </button>
  </td>
</template>
