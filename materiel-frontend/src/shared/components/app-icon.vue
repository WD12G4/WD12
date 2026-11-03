<script lang="ts">
/**
 * Icône vectorielle au trait, dessinée en SVG (aucune librairie d'icônes).
 * Décorative : masquée aux lecteurs d'écran, le texte ou l'aria-label du parent porte le sens.
 * Utilisé par : app-nav.vue, app-topbar.vue, use-nav-links.ts (type IconName)
 */

//----------------- VALEURS ------------------------------
/** Tracés SVG (grille 24 × 24) de chaque icône disponible. */
const iconPaths = {
  dashboard: 'M3 3h7v7H3zM14 3h7v7h-7zM14 14h7v7h-7zM3 14h7v7H3z',
  material: 'M3 4h18v12H3zM8 20h8M12 16v4',
  room: 'M3 21h18M6 21V3h12v18M14 12h.01',
  building: 'M4 21V5l8-3 8 3v16M2 21h20M9 8h.01M15 8h.01M9 12h.01M15 12h.01M10 21v-4h4v4',
  alert: 'M12 3 2 21h20L12 3zM12 10v4M12 17.5h.01',
  inbox: 'M3 13h5l2 3h4l2-3h5M5 5h14l2 8v6H3v-6z',
  activity: 'M3 12h4l3-8 4 16 3-8h4',
  users: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM22 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8',
  logout: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9',
  menu: 'M4 6h16M4 12h16M4 18h16',
} as const;

//----------------- TYPES --------------------------------
/** Nom d'une icône disponible. */
type IconName = keyof typeof iconPaths;

export type {IconName};
</script>

<script setup lang="ts">
import {computed} from 'vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Icône à afficher. */
  name: IconName;
}>();

//------------------ COMPUTED --------------------------------
/** Tracé de l'icône demandée. */
const path = computed(() => iconPaths[props.name]);
</script>

<template>
  <svg
      class="icon"
      viewBox="0 0 24 24"
      aria-hidden="true"
      focusable="false"
  >
    <path :d="path" />
  </svg>
</template>

<style scoped>
.icon {
  width: 1.25rem;
  height: 1.25rem;
  flex-shrink: 0;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}
</style>
