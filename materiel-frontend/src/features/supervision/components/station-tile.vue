<script setup lang="ts">
/**
 * Tuile d'un poste : en titre le stagiaire connecté (session ouverte, RG-30), sinon le nom du poste ;
 * en dessous le logo du système et le nom du poste (ou « Aucune session ouverte ») ; l'état ; les
 * applications ouvertes en icônes façon Dock (les 8 premières, puis
 * « +N » : le détail complet est dans le panneau).
 * Un clic sur la tuile ouvre le détail (panneau à droite), où l'on ferme une application (RG-26) :
 * les icônes de la tuile ne ferment rien, pour éviter une fermeture par un clic malheureux.
 * Une application fermée rétrécit puis disparaît de la tuile. Un poste simulé porte la marque
 * « Simulé » ; pendant sa masterisation, la tuile montre l'étape et une barre d'avancement.
 * Composant de présentation : émet des événements. La tuile s'incline vers la souris (use-motion.ts).
 * Utilisé par : station-grid.vue
 * RG : RG-25
 */
import {computed} from 'vue';
import type {WorkstationView} from '../composables/use-supervision.ts';
import {openAppsLabel, statusLabel} from '../composables/use-station-labels.ts';
import {resetTilt, tiltTowardsPointer} from '../../../shared/composables/use-motion.ts';
import AppGlyph from './app-glyph.vue';
import OsLogo from './os-logo.vue';

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
const openLabel = computed(() => `${[sessionUser.value, name.value].filter(Boolean).join(' sur ')}, ${status.value} : voir le détail`);
const progress = computed(() => props.view.workstation.reimage);
/** Stagiaire connecté (identifiant de session), ou null. */
const sessionUser = computed(() => props.view.workstation.sessionUser);
/** Titre : le stagiaire, sinon le nom du poste. */
const title = computed(() => sessionUser.value ?? name.value);
/** Sous le titre : le nom du poste s'il y a un stagiaire, « Aucune session ouverte » s'il est en ligne, rien s'il est éteint. */
const subtitle = computed(() => {
  if (sessionUser.value) return name.value;
  return props.view.workstation.status === 'online' ? 'Aucune session ouverte' : '';
});
const os = computed(() => props.view.workstation.os);
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
          {{ title }}
        </button>
      </h3>
      <span class="station-status">{{ status }}</span>
    </div>
    <p v-if="subtitle" class="station-meta">
      <OsLogo v-if="os" :os="os" />
      <span>{{ subtitle }}</span>
    </p>
    <span
        v-if="view.workstation.simulated"
        class="sim-badge"
        title="Poste simulé (démonstration) : ses applications sont fictives"
    >
      Simulé
    </span>
    <div v-if="progress" class="reimage-progress">
      <span>{{ progress.phase }}</span>
      <progress :value="progress.percent" max="100" />
    </div>
    <TransitionGroup
        v-else
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
