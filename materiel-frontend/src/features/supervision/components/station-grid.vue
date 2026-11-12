<script setup lang="ts">
/**
 * Postes de la salle en grille de tuiles, qui apparaissent en cascade.
 * Composant de présentation.
 * Utilisé par : supervision-view.vue
 */
import type {WorkstationView} from '../composables/use-supervision.ts';
import StationTile from './station-tile.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Postes à afficher, avec leurs applications. */
  views: WorkstationView[];
  /** Identifiant du poste ouvert dans le panneau, ou null. */
  selectedId: number | null;
}>();

const emit = defineEmits<{
  /** Ouverture du détail d'un poste (son identifiant). */
  open: [workstationId: number];
}>();

//------------------ FUNCTIONS -------------------------------
const isSelected = (view: WorkstationView): boolean => view.workstation.id === props.selectedId;
/** Rang d'apparition d'une tuile (variable CSS --i, lue par l'animation d'entrée). */
const cascade = (index: number): Record<string, number> => ({'--i': index});
</script>

<template>
  <p v-if="views.length === 0" class="empty-state">Aucun poste dans cette salle.</p>
  <ul v-else class="station-grid">
    <li
        v-for="(view, index) in views"
        :key="view.workstation.id"
        :style="cascade(index)"
    >
      <StationTile
          :view="view"
          :selected="isSelected(view)"
          @open="emit('open', $event.workstation.id)"
      />
    </li>
  </ul>
</template>
