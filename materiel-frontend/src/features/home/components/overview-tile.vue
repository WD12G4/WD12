<script setup lang="ts">
/**
 * Tuile cliquable de la vue d'ensemble : une salle, ou le stock.
 * Tout est visible sans rien déplier : total, répartition par type, alerte de fin de disponibilité.
 * Composant de présentation : un clic mène à la cible (fiche de la salle, liste filtrée).
 * Utilisé par : room-overview.vue
 * RG : RG-24 (alerte groupée, pas une alerte par matériel)
 */
import {computed} from 'vue';
import type {RouteTarget} from '../../../router/route-types.ts';
import type {MaterialSummary} from '../composables/use-room-overview.ts';
import Link from '../../../shared/components/Link.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Libellé en tête de tuile, ex. « Salle 10 » ou « En stock ». */
  title: string;
  /** Libellé secondaire, en petit au-dessus du titre, ex. « Bâtiment 101 ». */
  subtitle: string;
  /** Résumé du matériel. */
  summary: MaterialSummary;
  /** Page ouverte au clic. */
  to: RouteTarget;
  /** Style en pointillés, pour ce qui n'est pas une salle (le stock). */
  dashed?: boolean;
}>();

//------------------ COMPUTED --------------------------------
/** Classes de la tuile : pointillés pour le stock, atténuée si elle est vide. */
const tileClass = computed(() => ({dashed: props.dashed, empty: !props.summary.hasMaterials}));
</script>

<template>
  <Link
      class="overview-tile"
      :class="tileClass"
      :to="to"
  >
    <span class="overview-tile-subtitle">{{ subtitle }}</span>
    <span class="overview-tile-title">{{ title }}</span>
    <span class="overview-tile-count">
      <strong>{{ summary.total }}</strong> matériel(s)
    </span>
    <ul v-if="summary.hasMaterials" class="type-chips">
      <li v-for="typeCount in summary.byType" :key="typeCount.type">
        {{ typeCount.label }} <strong>{{ typeCount.count }}</strong>
      </li>
    </ul>
    <span v-if="summary.hasExpiring" class="badge expiring">
      {{ summary.expiring }} bientôt expiré(s)
    </span>
  </Link>
</template>
