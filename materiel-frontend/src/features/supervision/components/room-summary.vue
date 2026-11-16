<script setup lang="ts">
/**
 * Résumé de la salle, dans l'esprit de « Temps d'écran » : une barre compacte (postes, postes en ligne,
 * applications ouvertes) pour laisser les postes visibles sans défiler, applications ouvertes sur plusieurs postes avec « Fermer partout », et, pour
 * l'administrateur, la masterisation de plusieurs postes simulés à la fois.
 * Composant de présentation : émet des événements.
 * Utilisé par : supervision-view.vue
 * RG : RG-26 (« Fermer partout » : extension à faire valider par le client), RG-28
 */
import {computed} from 'vue';
import type {WorkstationView} from '../composables/use-supervision.ts';
import {roomFigures, sharedApps} from '../composables/use-room-activity.ts';
import AnimatedNumber from '../../../shared/components/animated-number.vue';
import AppGlyph from './app-glyph.vue';
import {plural} from '../../../shared/composables/use-plural.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Postes de la salle et leurs applications. */
  views: WorkstationView[];
  /** Vrai pendant une action. */
  busy: boolean;
  /** Vrai si l'utilisateur peut masteriser (ADMIN, RG-28). */
  canReimage: boolean;
}>();

const emit = defineEmits<{
  /** Fermer cette application sur tous les postes de la salle (nom du processus, nom affiché). */
  killEverywhere: [name: string, label: string];
  /** Ouvrir l'assistant de masterisation des postes simulés. */
  reimage: [];
}>();

//------------------ COMPUTED --------------------------------
/** Chiffres de la salle, chacun suivi de son libellé accordé. */
const figures = computed(() => {
  const {stations, online, openApps} = roomFigures(props.views);
  return [
    {key: 'stations', value: stations, label: plural(stations, 'poste')},
    {key: 'online', value: online, label: 'en ligne'},
    {key: 'apps', value: openApps, label: plural(openApps, 'application ouverte', 'applications ouvertes')},
  ];
});
const shared = computed(() => sharedApps(props.views));
const hasShared = computed(() => shared.value.length > 0);
const showReimage = computed(() => props.canReimage && props.views.some(view => view.workstation.simulated));
</script>

<template>
  <section class="room-summary" aria-label="Résumé de la salle">
    <div class="summary-bar">
      <dl class="summary-figures">
        <div v-for="figure in figures" :key="figure.key">
          <dt>{{ figure.label }}</dt>
          <dd><AnimatedNumber :value="figure.value" /></dd>
        </div>
      </dl>
      <button
          v-if="showReimage"
          type="button"
          class="secondary-button"
          :disabled="busy"
          @click="emit('reimage')"
      >
        Masteriser des postes simulés…
      </button>
    </div>
    <div v-if="hasShared" class="shared-apps">
      <h3 class="eyebrow">Ouvertes sur plusieurs postes</h3>
      <ul>
        <li v-for="app in shared" :key="app.name">
          <AppGlyph :name="app.name" :icon="app.icon" />
          <span class="shared-app-text">
            <strong>{{ app.displayName }}</strong>
            <span class="muted">{{ app.stations }} postes</span>
          </span>
          <button
              type="button"
              class="secondary-button"
              :disabled="busy"
              @click="emit('killEverywhere', app.name, app.displayName)"
          >
            Fermer partout
          </button>
        </li>
      </ul>
    </div>
  </section>
</template>
