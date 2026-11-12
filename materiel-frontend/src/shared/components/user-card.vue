<script setup lang="ts">
/**
 * Bloc de l'utilisateur connecté dans le menu : pastille, nom et rôle, bouton de déconnexion.
 * En mode démonstration, la pastille, le nom et le rôle deviennent la liste de changement de compte
 * (dev-user-switch.vue), chargée seulement dans ce mode.
 * Mode démonstration : actif en développement et, par défaut, dans la version construite (présentation
 * sur la VM). Pour une vraie mise en production, construire avec VITE_DEMO_ACCOUNTS=false : les comptes
 * de démonstration et leurs mots de passe sont alors retirés du build (condition écrite telle quelle
 * pour que Vite la remplace par une constante).
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
/** Changement de compte : null hors mode démonstration (import retiré du build). */
const DevUserSwitch = import.meta.env.DEV || import.meta.env.VITE_DEMO_ACCOUNTS !== 'false'
    ? defineAsyncComponent(() => import('./dev-user-switch.vue')) : null;

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
