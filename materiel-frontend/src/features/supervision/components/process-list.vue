<script setup lang="ts">
/**
 * Processus lancés par l'utilisateur d'un poste (hors processus système, RG-25).
 * Composant de présentation.
 * Utilisé par : workstation-item.vue
 */
import type {ProcessInfo} from '../../../models/supervision.model.ts';
import ProcessItem from './process-item.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Processus du poste. */
  processes: ProcessInfo[];
  /** Vrai pendant une action. */
  busy: boolean;
}>();

const emit = defineEmits<{
  /** Demande d'arrêt d'un processus. */
  kill: [process: ProcessInfo];
}>();
</script>

<template>
  <p v-if="processes.length === 0" class="muted">Aucun processus lancé.</p>
  <ul v-else class="process-list">
    <li v-for="process in processes" :key="process.pid">
      <ProcessItem
          :process="process"
          :busy="busy"
          @kill="emit('kill', $event)"
      />
    </li>
  </ul>
</template>

<style scoped>
.process-list {
  margin: 0.5rem 0 0;
  padding: 0;
  list-style: none;
}

.process-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.4rem 0;
  border-top: 1px solid var(--border);
}
</style>
