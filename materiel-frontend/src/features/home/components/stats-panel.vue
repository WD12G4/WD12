<script setup lang="ts">
/**
 * Compteurs du tableau de bord, chacun menant à la liste du matériel, déjà filtrée quand c'est possible.
 * Formateur (roomId) : seulement le matériel de sa salle, vers la fiche de la salle.
 * Le compteur des fins de disponibilité (expiry-indicator.vue, horizon réglable) arrive par le slot, en fin de rangée.
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
  /** Salle de cours du formateur, ou null (tout le parc). */
  roomId: number | null;
}>();

//------------------ COMPUTED --------------------------------
/** Un compteur : sa valeur, son libellé et la liste qu'il ouvre. RG-07 (affecté / en stock), RG-24. */
const items = computed<{key: string; value: number; label: string; to: RouteTarget}[]>(() => (props.roomId !== null ? roomItems(props.roomId) : [
  {key: 'active', value: props.stats.active, label: 'Matériels suivis', to: {name: 'materials'}},
  {key: 'assigned', value: props.stats.assigned, label: 'Affectés à une salle', to: {name: 'materials', query: {emplacement: 'salle'}}},
  {key: 'stock', value: props.stats.stock, label: 'En stock', to: {name: 'materials', query: {emplacement: 'stock'}}},
]));

//------------------ FUNCTIONS -------------------------------
/**
 * Compteurs de la salle du formateur : « affecté » et « en stock » n'ont pas de sens pour une seule salle.
 * @param roomId salle de cours
 */
function roomItems(roomId: number): {key: string; value: number; label: string; to: RouteTarget}[] {
  const to: RouteTarget = {name: 'room-detail', params: {id: roomId}};
  return [{key: 'active', value: props.stats.active, label: 'Matériels de votre salle', to}];
}
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
    <slot />
  </div>
</template>
