<script setup lang="ts">
/**
 * Logo du système d'un poste (Windows, macOS, Linux), d'une seule couleur (currentColor), comme
 * les symboles d'Apple : il prend la couleur du texte autour, dans tous les thèmes.
 * Linux : manchot simplifié (ventre et yeux évidés).
 * Composant de présentation.
 * Utilisé par : station-tile.vue, workstation-panel.vue
 */
import {computed} from 'vue';
import {type WorkstationOs, workstationOsLabels} from '../../../models/supervision.model.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Système du poste. */
  os: WorkstationOs;
}>();

//------------------ DATAS -----------------------------------
/** Tracés (grille 24 × 24), remplis ; les parties évidées utilisent la règle evenodd. */
const logoPaths: Readonly<Record<WorkstationOs, string[]>> = {
  windows: ['M0 3.45 9.75 2.1v9.45H0zM10.95 1.95 24 0v11.4H10.95zM0 12.6h9.75v9.45L0 20.7zM10.95 12.6H24V24l-13.05-1.8z'],
  macos: [
    'M12.15 6.9c-.95 0-2.42-1.08-3.96-1.04-2.04.03-3.91 1.18-4.96 3.01-2.12 3.68-.55 9.1 1.52 12.09 1.01 1.45 2.2 3.09 '
    + '3.79 3.04 1.52-.07 2.09-.99 3.94-.99 1.83 0 2.35.99 3.96.95 1.64-.03 2.68-1.48 3.68-2.95 1.16-1.69 1.64-3.33 '
    + '1.66-3.42-.04-.01-3.18-1.22-3.22-4.86-.03-3.04 2.48-4.49 2.6-4.56-1.43-2.09-3.62-2.32-4.39-2.38-2-.16-3.68 1.09-4.61 1.09z',
    'M15.53 3.83c.84-1.01 1.4-2.43 1.25-3.83-1.21.05-2.66.8-3.53 1.82-.78.9-1.45 2.34-1.27 3.71 1.34.1 2.71-.69 3.55-1.7z',
  ],
  linux: [
    'M12 1.5c-2.7 0-4.1 2.3-4.1 5.2 0 1.6-.6 2.9-1.7 4.5C5 13.2 3.8 15.3 3.8 17.6c0 3.2 3.6 4.6 8.2 4.6s8.2-1.4 8.2-4.6c0-2.3-1.2-4.4-2.4-6.4'
    + '-1.1-1.6-1.7-2.9-1.7-4.5 0-2.9-1.4-5.2-4.1-5.2zM7.8 15.7a4.2 5 0 1 0 8.4 0 4.2 5 0 1 0-8.4 0zM9.3 6.5a1.1 1.4 0 1 0 2.2 0 '
    + '1.1 1.4 0 1 0-2.2 0zM12.5 6.5a1.1 1.4 0 1 0 2.2 0 1.1 1.4 0 1 0-2.2 0z',
    'M4.6 21.4c0-1 1.7-1.9 3.6-1.9s3.1.8 3.1 1.7-1.5 1.7-3.4 1.7-3.3-.6-3.3-1.5zM19.4 21.4c0-1-1.7-1.9-3.6-1.9s-3.1.8-3.1 1.7 1.5 1.7 3.4 1.7 3.3-.6 3.3-1.5z',
  ],
};

//------------------ COMPUTED --------------------------------
const paths = computed(() => logoPaths[props.os]);
const label = computed(() => workstationOsLabels[props.os]);
</script>

<template>
  <svg
      class="os-logo"
      viewBox="0 0 24 24"
      role="img"
      :aria-label="label"
  >
    <title>{{ label }}</title>
    <path
        v-for="(d, index) in paths"
        :key="index"
        :d="d"
        fill="currentColor"
        fill-rule="evenodd"
    />
  </svg>
</template>
