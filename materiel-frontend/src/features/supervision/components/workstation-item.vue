<script setup lang="ts">
/**
 * Un poste de la salle : nom, stagiaire, identifiant réseau, processus et masterisation.
 * Composant de présentation : les droits arrivent par la prop canReimage.
 * Utilisé par : workstation-list.vue
 * RG : RG-28 (masterisation par l'administrateur), RG-29, RG-30
 */
import type {ProcessInfo} from '../../../models/supervision.model.ts';
import type {WorkstationView} from '../composables/use-supervision.ts';
import ProcessList from './process-list.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Poste et ses processus. */
  view: WorkstationView;
  /** Vrai si l'utilisateur peut masteriser (ADMIN). */
  canReimage: boolean;
  /** Vrai pendant une action. */
  busy: boolean;
}>();

const emit = defineEmits<{
  /** Demande d'arrêt d'un processus. */
  kill: [process: ProcessInfo];
  /** Demande de masterisation du poste. */
  reimage: [view: WorkstationView];
}>();
</script>

<template>
  <div class="list-header">
    <h3>{{ view.workstation.name }} — {{ view.workstation.traineeLabel }}</h3>
    <span class="muted">MAC {{ view.workstation.macAddress }}</span>
  </div>
  <ProcessList
      :processes="view.processes"
      :busy="busy"
      @kill="emit('kill', $event)"
  />
  <div v-if="canReimage" class="actions">
    <button
        type="button"
        class="secondary-button"
        :disabled="busy"
        @click="emit('reimage', view)"
    >
      Masteriser le poste
    </button>
  </div>
</template>

<style scoped>
.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}
</style>
