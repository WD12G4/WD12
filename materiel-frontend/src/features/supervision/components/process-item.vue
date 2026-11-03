<script setup lang="ts">
/**
 * Un processus lancé sur un poste, avec le bouton pour l'arrêter.
 * Composant de présentation.
 * Utilisé par : process-list.vue
 * RG : RG-26 (arrêt en un clic)
 */
import {computed} from 'vue';
import type {ProcessInfo} from '../../../models/supervision.model.ts';
import {formatDateTime} from '../../../shared/composables/use-dates.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Processus affiché. */
  process: ProcessInfo;
  /** Vrai pendant une action, pour éviter les doubles clics. */
  busy: boolean;
}>();

const emit = defineEmits<{
  /** Demande d'arrêt du processus. */
  kill: [process: ProcessInfo];
}>();

//------------------ COMPUTED --------------------------------
const startedAt = computed(() => `lancé le ${formatDateTime(props.process.startedAt)}`);
</script>

<template>
  <span>{{ process.name }}</span>
  <span class="muted">{{ startedAt }}</span>
  <button
      type="button"
      class="delete-button"
      :disabled="busy"
      @click="emit('kill', process)"
  >
    Arrêter
  </button>
</template>
