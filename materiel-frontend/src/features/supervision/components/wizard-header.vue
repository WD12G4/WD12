<script setup lang="ts">
/**
 * En-tête de l'assistant de masterisation : étape en cours, titre, bouton fermer et fil des étapes
 * (faites, en cours, à venir).
 * Composant de présentation.
 * Utilisé par : reimage-wizard.vue
 */
import {computed} from 'vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Libellés des étapes, dans l'ordre. */
  steps: readonly string[];
  /** Rang de l'étape en cours (à partir de 0). */
  step: number;
  /** id du titre, relié à aria-labelledby de la fenêtre. */
  titleId: string;
}>();

const emit = defineEmits<{
  /** Fermeture de l'assistant. */
  close: [];
}>();

//------------------ COMPUTED --------------------------------
const title = computed(() => props.steps[props.step]);
const position = computed(() => `Masterisation simulée · étape ${props.step + 1} sur ${props.steps.length}`);

//------------------ FUNCTIONS -------------------------------
const stepClass = (index: number): Record<string, boolean> => ({done: index < props.step, current: index === props.step});
</script>

<template>
  <header class="wizard-header">
    <div>
      <span class="eyebrow">{{ position }}</span>
      <h2 :id="titleId">{{ title }}</h2>
    </div>
    <button
        type="button"
        class="icon-button"
        aria-label="Fermer l'assistant"
        @click="emit('close')"
    >
      ×
    </button>
  </header>
  <ol class="wizard-steps" aria-hidden="true">
    <li
        v-for="(label, index) in steps"
        :key="label"
        :class="stepClass(index)"
    >
      {{ label }}
    </li>
  </ol>
</template>
