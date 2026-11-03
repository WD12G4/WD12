<script setup lang="ts">
/**
 * Indicateur groupé des fins de disponibilité proches.
 * RG-24 : une seule indication pour tout le parc, jamais une alerte par matériel.
 * Composant de présentation.
 * Utilisé par : home-view.vue
 */
import {EXPIRY_WARNING_DAYS} from '../../../shared/composables/use-dates.ts';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Nombre de matériels concernés. */
  count: number;
}>();
</script>

<template>
  <p
      v-if="count > 0"
      class="flash error"
      role="status"
  >
    {{ count }} matériel(s) arrivent en fin de disponibilité dans les {{ EXPIRY_WARNING_DAYS }} prochains jours
    (ou l'ont dépassée).
  </p>
</template>
