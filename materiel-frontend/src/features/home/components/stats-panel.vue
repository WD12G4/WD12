<script setup lang="ts">
/**
 * Compteurs du tableau de bord, chacun menant à la liste du matériel, déjà filtrée quand c'est possible.
 * Les chiffres défilent jusqu'à leur valeur ; les cartes apparaissent en cascade (--i) et
 * s'inclinent légèrement vers la souris (use-motion.ts).
 * Composant de présentation.
 * Utilisé par : home-view.vue
 */
import {computed} from 'vue';
import type {RouteTarget} from '../../../router/route-types.ts';
import type {DashboardStats} from '../composables/use-dashboard-stats.ts';
import {resetTilt, tiltTowardsPointer} from '../../../shared/composables/use-motion.ts';
import AnimatedNumber from '../../../shared/components/animated-number.vue';
import Link from '../../../shared/components/Link.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Compteurs à afficher. */
  stats: DashboardStats;
  /** Horizon des fins de disponibilité, en jours (réglé dans le bandeau sous les compteurs). */
  horizon: number;
}>();

//------------------ COMPUTED --------------------------------
/** Un compteur : sa valeur, son libellé et la liste qu'il ouvre. RG-07 (affecté / en stock), RG-24. */
const items = computed<{key: string; value: number; label: string; to: RouteTarget}[]>(() => [
  {key: 'active', value: props.stats.active, label: 'Matériels suivis', to: {name: 'materials'}},
  {key: 'assigned', value: props.stats.assigned, label: 'Affectés à une salle', to: {name: 'materials', query: {emplacement: 'salle'}}},
  {key: 'stock', value: props.stats.stock, label: 'En stock', to: {name: 'materials', query: {emplacement: 'stock'}}},
  {key: 'expiring', value: props.stats.expiring, label: `Fin de disponibilité sous ${props.horizon} j`, to: {name: 'materials'}},
]);
</script>

<template>
  <div class="stats">
    <Link
        v-for="(item, index) in items"
        :key="item.key"
        class="stat tilt"
        :style="{ '--i': index }"
        :to="item.to"
        @pointermove="tiltTowardsPointer"
        @pointerleave="resetTilt"
    >
      <strong><AnimatedNumber :value="item.value" /></strong>
      <span>{{ item.label }}</span>
    </Link>
  </div>
</template>
