<script setup lang="ts">
/**
 * Choix unique sous forme de cartes (système, image de base de l'assistant de masterisation) :
 * groupe de boutons radio accessible, la carte choisie est mise en avant.
 * Composant de présentation.
 * Utilisé par : reimage-wizard.vue
 */
import type {ChoiceOption} from '../composables/use-reimage-wizard.ts';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Cartes proposées. */
  options: ChoiceOption[];
  /** Valeur choisie. */
  selected: string;
  /** Nom accessible du groupe (« Système »). */
  label: string;
}>();

const emit = defineEmits<{
  /** Une carte est choisie. */
  choose: [value: string];
}>();
</script>

<template>
  <div
      class="choice-cards"
      role="radiogroup"
      :aria-label="label"
  >
    <button
        v-for="option in options"
        :key="option.value"
        type="button"
        role="radio"
        class="choice-card"
        :aria-checked="option.value === selected"
        @click="emit('choose', option.value)"
    >
      <strong>{{ option.label }}</strong>
      <span>{{ option.detail }}</span>
    </button>
  </div>
</template>
