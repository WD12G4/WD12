<script setup lang="ts">
/**
 * Compteur des fins de disponibilité à venir, avec un horizon réglable (30, 60 ou 90 jours) : le nombre
 * se met à jour aussitôt, sans recharger (idée reprise des calculateurs interactifs). Toujours affiché,
 * même à 0 ; le chiffre passe en rouge s'il y a des matériels concernés. Placé dans la rangée des
 * compteurs (slot de stats-panel.vue), pour ne pas répéter le même nombre dans un bandeau à part.
 * Composant de présentation : l'horizon est lié en v-model:horizon à home-view.vue.
 * Utilisé par : home-view.vue
 * RG : RG-24 (alerte groupée sur le tableau de bord, jamais une alerte par matériel)
 */
import {computed} from 'vue';
import type {SelectOption} from '../../../shared/composables/use-listbox.ts';
import AnimatedNumber from '../../../shared/components/animated-number.vue';
import SegmentedControl from '../../../shared/components/segmented-control.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Nombre de matériels concernés pour l'horizon choisi (échéance dépassée comprise). */
  count: number;
}>();

/** Horizon en jours. */
const horizon = defineModel<number>('horizon', {required: true});

//------------------ DATAS -----------------------------------
const horizonOptions: SelectOption<number>[] = [30, 60, 90].map(days => ({value: days, label: `${days} j`}));

//------------------ COMPUTED --------------------------------
/** Chiffre en rouge s'il y a des matériels concernés, neutre sinon. */
const statClass = computed(() => ({alert: props.count > 0}));
</script>

<template>
  <section
      class="stat expiry-stat"
      :class="statClass"
      aria-live="polite"
  >
    <div class="expiry-stat-head">
      <strong><AnimatedNumber :value="count" /></strong>
      <SegmentedControl
          v-model="horizon"
          :options="horizonOptions"
          labelledby="expiry-horizon-label"
      />
    </div>
    <span id="expiry-horizon-label">Fins de disponibilité, dépassées comprises</span>
  </section>
</template>
