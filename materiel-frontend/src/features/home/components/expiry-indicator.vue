<script setup lang="ts">
/**
 * Fins de disponibilité à venir, avec un horizon réglable (30, 60 ou 90 jours) : le nombre se met à
 * jour aussitôt, sans recharger (idée reprise des calculateurs interactifs). Toujours affiché, même à 0.
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
/** Rouge s'il y a des matériels concernés, neutre sinon. */
const panelClass = computed(() => ({alert: props.count > 0}));
const message = computed(() => {
  if (props.count === 0) return 'aucun matériel concerné.';
  return props.count === 1 ? 'matériel concerné (échéance dépassée comprise).' : 'matériels concernés (échéances dépassées comprises).';
});
</script>

<template>
  <section
      class="expiry-panel"
      :class="panelClass"
      aria-live="polite"
  >
    <span id="expiry-horizon-label" class="expiry-label">Fins de disponibilité dans les</span>
    <SegmentedControl
        v-model="horizon"
        :options="horizonOptions"
        labelledby="expiry-horizon-label"
    />
    <p class="expiry-count">
      <strong><AnimatedNumber :value="count" /></strong> {{ message }}
    </p>
  </section>
</template>
