<script setup lang="ts">
/**
 * Barre du haut : marque, état de la liaison, actions en attente, agent connecté, déconnexion.
 * Composant de présentation : App.vue fournit l'état.
 * Utilisé par : App.vue
 */
import {computed} from 'vue';
import {roleLabels, type User} from '../../models/user.model.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  user: User;
  /** Vrai si l'API est joignable et la session en ligne. */
  online: boolean;
  /** Actions gardées sur le téléphone. */
  pendingCount: number;
  /** Actions refusées par l'API, à traiter. */
  blockedCount: number;
}>();

const emit = defineEmits<{
  logout: [];
}>();

//------------------ COMPUTED --------------------------------
const syncKind = computed(() => (props.blockedCount > 0 ? 'danger' : props.pendingCount > 0 ? 'warn' : 'muted'));
</script>

<template>
  <header class="status-bar">
    <RouterLink :to="{name: 'scan'}" class="brand mono">⌖ RECON</RouterLink>
    <span class="pill" :class="online ? 'ok' : 'warn'">{{ online ? 'EN LIGNE' : 'HORS LIGNE' }}</span>
    <RouterLink :to="{name: 'sync'}" class="pill" :class="syncKind" :aria-label="`${pendingCount} action(s) en attente`">
      ⇅ {{ pendingCount }}
    </RouterLink>
    <button type="button" class="ghost small user" :aria-label="`Déconnecter ${user.username}`"
            :title="roleLabels[user.role]" @click="emit('logout')">
      <span class="user-name">{{ user.username }}</span> <span aria-hidden="true">⏻</span>
    </button>
  </header>
</template>
