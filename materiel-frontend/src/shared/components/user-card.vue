<script setup lang="ts">
/**
 * Bloc de l'utilisateur connecté dans le menu : pastille, nom et rôle, bouton de déconnexion.
 * En mode développement, la pastille, le nom et le rôle deviennent la liste de changement de compte
 * (dev-user-switch.vue), chargée seulement dans ce mode : le build de production ne la contient pas.
 * Composant de présentation.
 * Utilisé par : app-nav.vue
 */
import {computed, defineAsyncComponent} from 'vue';
import {roleLabels, type User} from '../../models/user.model.ts';
import AppIcon from './app-icon.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Utilisateur connecté. */
  user: User;
}>();

const emit = defineEmits<{
  /** L'utilisateur demande à se déconnecter. */
  logout: [];
}>();

//------------------ DATAS -----------------------------------
/** Outil de test : null hors mode développement (import retiré du build de production). */
const DevUserSwitch = import.meta.env.DEV ? defineAsyncComponent(() => import('./dev-user-switch.vue')) : null;

//------------------ COMPUTED --------------------------------
/** Rôle affiché par son libellé, jamais par son code. */
const roleLabel = computed(() => roleLabels[props.user.role]);

/** Initiale affichée dans la pastille de l'utilisateur. */
const initial = computed(() => props.user.username.charAt(0).toUpperCase());
</script>

<template>
  <div class="user-card">
    <component
        :is="DevUserSwitch"
        v-if="DevUserSwitch"
        :user="user"
    />
    <template v-else>
      <span class="avatar" aria-hidden="true">{{ initial }}</span>
      <div class="user-meta">
        <strong>{{ user.username }}</strong>
        <span>{{ roleLabel }}</span>
      </div>
    </template>
    <button
        type="button"
        class="icon-button"
        aria-label="Se déconnecter"
        title="Se déconnecter"
        @click="emit('logout')"
    >
      <AppIcon name="logout" />
    </button>
  </div>
</template>
