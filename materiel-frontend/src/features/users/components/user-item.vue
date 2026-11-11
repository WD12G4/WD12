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
  /** Vrai s'il s'agit du compte connecté : il ne peut pas s'archiver lui-même. */
  isCurrentUser: boolean;
}>();

const emit = defineEmits<{
  /** Demande de modification. */
  edit: [user: User];
  /** Demande d'archivage (compte actif) ou de réactivation (compte archivé). */
  toggleArchive: [user: User];
}>();

//------------------ COMPUTED --------------------------------
/** RG-19 : l'état est affiché en texte, pas seulement en couleur. Un compte archivé ne peut plus se connecter. */
const statusLabel = computed(() => (props.user.enabled ? 'Actif' : 'Archivé'));
const statusClass = computed(() => (props.user.enabled ? 'assigned' : 'archived'));
const archiveLabel = computed(() => (props.user.enabled ? 'Archiver' : 'Réactiver'));
/** Archiver : bouton rouge ; réactiver : bouton secondaire. */
const archiveClass = computed(() => (props.user.enabled ? 'delete-button' : 'secondary-button'));
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
        :class="archiveClass"
        @click="emit('toggleArchive', user)"
    >
      {{ archiveLabel }}
    </button>
  </td>
</template>
