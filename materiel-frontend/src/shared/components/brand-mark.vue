<script setup lang="ts">
/**
 * Logo de BASTION : l'œil (contour en amande, pupille à quatre carrés, l'un vert), d'après le logo
 * fourni. Les trois carrés clairs sont évidés (masque) : ils laissent voir le fond, quel que soit le
 * thème. Couleurs par thème : --logo-ink, --logo-accent (style.css).
 * Composant de présentation, décoratif (le nom est écrit à côté).
 * Utilisé par : app-nav.vue, app-topbar.vue, login-view.vue, room-choice-view.vue
 */
import {useId} from 'vue';

//------------------ DATAS -----------------------------------
/** Identifiant unique du masque (le logo peut apparaître plusieurs fois dans la page). */
const maskId = useId();
/** Carrés de la pupille (évidés), dans la grille du logo d'origine (200 × 200). */
const holes = [{x: 77, y: 77}, {x: 77, y: 104}, {x: 104, y: 104}, {x: 104, y: 77}] as const;
</script>

<template>
  <svg
      class="brand-mark"
      viewBox="4 44 192 112"
      aria-hidden="true"
  >
    <!-- Masque : blanc = visible, noir = évidé (valeurs de masque, pas des couleurs affichées) -->
    <mask :id="maskId">
      <rect
          width="200"
          height="200"
          fill="white"
      />
      <rect
          v-for="hole in holes"
          :key="`${hole.x}-${hole.y}`"
          :x="hole.x"
          :y="hole.y"
          width="19"
          height="19"
          rx="5"
          fill="black"
      />
    </mask>
    <path class="brand-mark-outline" d="M12 100 Q100 6 188 100 Q100 194 12 100 Z" />
    <circle
        class="brand-mark-pupil"
        cx="100"
        cy="100"
        r="40"
        :mask="`url(#${maskId})`"
    />
    <rect
        class="brand-mark-accent"
        x="104"
        y="77"
        width="19"
        height="19"
        rx="5"
    />
  </svg>
</template>
