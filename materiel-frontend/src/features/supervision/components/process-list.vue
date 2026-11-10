<script setup lang="ts">
/**
 * Applications lancées par l'utilisateur d'un poste (hors processus système, RG-25).
 * Composant de présentation.
 * Utilisé par : workstation-panel.vue
 */
import type {ProcessInfo} from '../../../models/supervision.model.ts';
import ProcessItem from './process-item.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Applications du poste. */
  processes: ProcessInfo[];
  /** Message affiché s'il n'y en a aucune. */
  emptyLabel: string;
  /** Vrai pendant une action. */
  busy: boolean;
}>();

const emit = defineEmits<{
  /** Demande d'arrêt d'une application. */
  kill: [process: ProcessInfo];
}>();
</script>

<template>
  <p v-if="processes.length === 0" class="muted">{{ emptyLabel }}</p>
  <TransitionGroup
      v-else
      tag="ul"
      name="app-pop"
      class="process-list"
  >
    <li v-for="process in processes" :key="process.pid">
      <ProcessItem
          :process="process"
          :busy="busy"
          @kill="emit('kill', $event)"
      />
    </li>
  </TransitionGroup>
</template>
