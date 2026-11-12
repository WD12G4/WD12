<script setup lang="ts">
/**
 * Tuile d'un poste : nom, état et applications ouvertes en icônes façon Dock (les 8 premières, puis
 * « +N » : le détail complet est dans le panneau).
 * Un clic sur la tuile ouvre le détail (panneau à droite), où l'on ferme une application (RG-26) :
 * les icônes de la tuile ne ferment rien, pour éviter une fermeture par un clic malheureux.
 * Une application fermée rétrécit puis disparaît de la tuile.
 * Composant de présentation : émet des événements. La tuile s'incline vers la souris (use-motion.ts).
 * Utilisé par : station-grid.vue
 * RG : RG-25
 */
import {computed} from 'vue';
import type {WorkstationView} from '../composables/use-supervision.ts';
import {openAppsLabel, statusLabel} from '../composables/use-station-labels.ts';
import {resetTilt, tiltTowardsPointer} from '../../../shared/composables/use-motion.ts';
import AppGlyph from './app-glyph.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Poste et ses applications. */
  view: WorkstationView;
  /** Vrai si le poste est affiché dans le panneau de détail. */
  selected: boolean;
}>();

const emit = defineEmits<{
  /** Ouverture du détail du poste. */
  open: [view: WorkstationView];
}>();

//------------------ DATAS -----------------------------------
/** Icônes affichées au plus dans la tuile. */
const MAX_DOCK_ICONS = 8;

//------------------ COMPUTED --------------------------------
const name = computed(() => props.view.workstation.name);
/** Classes : état du poste (online, offline, reimaging) et poste ouvert dans le panneau. */
const tileClass = computed(() => [props.view.workstation.status, {selected: props.selected}]);
const dockProcesses = computed(() => props.view.processes.slice(0, MAX_DOCK_ICONS));
/** « +5 » quand toutes les applications ne tiennent pas dans la tuile, sinon chaîne vide. */
const moreLabel = computed(() => {
  const hidden = props.view.processes.length - MAX_DOCK_ICONS;
  return hidden > 0 ? `+${hidden}` : '';
});
const status = computed(() => statusLabel(props.view.workstation));
const countLabel = computed(() => openAppsLabel(props.view.workstation, props.view.processes.length));
const openLabel = computed(() => `${name.value}, ${status.value} : voir le détail`);
</script>

<template>
  <article
      class="station-tile tilt"
      :class="tileClass"
      @pointermove="tiltTowardsPointer"
      @pointerleave="resetTilt"
  >
    <div class="station-head">
      <h3 class="station-name">
        <button
            type="button"
            class="station-open"
            :aria-label="openLabel"
            @click="emit('open', view)"
        >
          {{ name }}
        </button>
      </h3>
      <span class="station-status">{{ status }}</span>
    </div>
    <TransitionGroup
        tag="ul"
        name="app-pop"
        class="app-dock"
    >
      <li v-for="process in dockProcesses" :key="process.pid">
        <AppGlyph :name="process.name" />
        <span class="visually-hidden">{{ process.name }}</span>
      </li>
      <li
          v-if="moreLabel"
          key="more"
          class="app-more"
      >
        {{ moreLabel }}
      </li>
    </TransitionGroup>
    <p class="station-count">{{ countLabel }}</p>
  </article>
</template>
