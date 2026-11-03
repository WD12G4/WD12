<script setup lang="ts">
/**
 * Menu principal : liens autorisés, utilisateur connecté et bouton de déconnexion.
 * Composant de présentation : les liens arrivent déjà filtrés selon le rôle (use-nav-links.ts).
 * Utilisé par : App.vue
 */
import {computed} from 'vue';
import {roleLabels, type User} from '../../models/user.model.ts';
import type {NavLink} from '../composables/use-nav-links.ts';
import Link from './Link.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Liens à afficher. */
  links: readonly NavLink[];
  /** Utilisateur connecté. */
  user: User;
}>();

const emit = defineEmits<{
  /** L'utilisateur demande à se déconnecter. */
  logout: [];
}>();

//------------------ COMPUTED --------------------------------
/** « jdupont (Gestionnaire) » : le rôle est affiché par son libellé, jamais par son code. */
const userLabel = computed(() => `${props.user.username} (${roleLabels[props.user.role]})`);
</script>

<template>
  <nav class="nav" aria-label="Menu principal">
    <Link
        v-for="link in links"
        :key="link.to.name"
        :to="link.to"
    >
      {{ link.label }}
    </Link>
  </nav>
  <div class="session-bar">
    <span class="muted">Connecté : {{ userLabel }}</span>
    <button
        type="button"
        class="secondary-button"
        @click="emit('logout')"
    >
      Se déconnecter
    </button>
  </div>
</template>
