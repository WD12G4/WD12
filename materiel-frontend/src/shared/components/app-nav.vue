<script setup lang="ts">
/**
 * Barre latérale : marque, liens autorisés, utilisateur connecté et déconnexion.
 * Sur mobile, elle devient un tiroir ouvert par le bouton menu de app-topbar.vue.
 * Composant de présentation : les liens arrivent déjà filtrés selon le rôle (use-nav-links.ts).
 * Utilisé par : App.vue
 */
import {computed} from 'vue';
import {useRoute} from 'vue-router';
import type {RouteNames} from '../../router/route-types.ts';
import {roleLabels, type User} from '../../models/user.model.ts';
import type {NavLink} from '../composables/use-nav-links.ts';
import AppIcon from './app-icon.vue';
import Link from './Link.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Liens à afficher. */
  links: readonly NavLink[];
  /** Utilisateur connecté. */
  user: User;
  /** Vrai si le tiroir est ouvert (mobile uniquement). */
  open: boolean;
}>();

const emit = defineEmits<{
  /** L'utilisateur demande à se déconnecter. */
  logout: [];
  /** L'utilisateur ferme le tiroir (clic sur le fond, mobile). */
  close: [];
}>();

//------------------ DATAS -----------------------------------
const route = useRoute();

//------------------ COMPUTED --------------------------------
/** Rôle affiché par son libellé, jamais par son code. */
const roleLabel = computed(() => roleLabels[props.user.role]);

/** Initiale affichée dans la pastille de l'utilisateur. */
const initial = computed(() => props.user.username.charAt(0).toUpperCase());

//------------------ FUNCTIONS -------------------------------
/** Vrai si la page courante appartient à la rubrique du lien (ex. fiche d'une salle → « Salles »). */
function isCurrent(link: NavLink): boolean {
  return link.section.includes(route.name as RouteNames);
}
</script>

<template>
  <aside
      id="app-sidebar"
      class="sidebar"
      :class="{ open }"
  >
    <!-- Marque -->
    <div class="brand">
      <span class="brand-logo" aria-hidden="true">GP</span>
      <span class="brand-name">Gestion du parc</span>
    </div>

    <!-- Liens -->
    <nav class="nav" aria-label="Menu principal">
      <Link
          v-for="link in links"
          :key="link.to.name"
          :to="link.to"
          :class="{ current: isCurrent(link) }"
      >
        <AppIcon :name="link.icon" />
        <span>{{ link.label }}</span>
      </Link>
    </nav>

    <!-- Utilisateur connecté -->
    <div class="user-card">
      <span class="avatar" aria-hidden="true">{{ initial }}</span>
      <div class="user-meta">
        <strong>{{ user.username }}</strong>
        <span>{{ roleLabel }}</span>
      </div>
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
  </aside>

  <!-- Fond cliquable derrière le tiroir (mobile) -->
  <Transition name="fade">
    <div
        v-if="open"
        class="sidebar-backdrop"
        @click="emit('close')"
    />
  </Transition>
</template>
