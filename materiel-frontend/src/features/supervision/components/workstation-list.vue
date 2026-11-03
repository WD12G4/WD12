<script setup lang="ts">
/**
 * Postes de la salle choisie.
 * Composant de présentation.
 * Utilisé par : supervision-view.vue
 */
import type {ProcessInfo} from '../../../models/supervision.model.ts';
import type {WorkstationView} from '../composables/use-supervision.ts';
import WorkstationItem from './workstation-item.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Postes à afficher, avec leurs processus. */
  views: WorkstationView[];
  /** Vrai si l'utilisateur peut masteriser (ADMIN). */
  canReimage: boolean;
  /** Vrai pendant une action. */
  busy: boolean;
}>();

const emit = defineEmits<{
  /** Demande d'arrêt d'un processus. */
  kill: [process: ProcessInfo];
  /** Demande de masterisation d'un poste. */
  reimage: [view: WorkstationView];
}>();
</script>

<template>
  <p v-if="views.length === 0" class="empty-state">Choisissez une salle pour voir ses postes.</p>
  <ul v-else class="plain-list">
    <li v-for="view in views" :key="view.workstation.id">
      <WorkstationItem
          :view="view"
          :can-reimage="canReimage"
          :busy="busy"
          @kill="emit('kill', $event)"
          @reimage="emit('reimage', $event)"
      />
    </li>
  </ul>
</template>
