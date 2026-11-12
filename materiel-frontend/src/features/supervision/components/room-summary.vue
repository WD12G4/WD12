<script setup lang="ts">
/**
 * Résumé de la salle, dans l'esprit de « Temps d'écran » : postes, postes en ligne, applications
 * ouvertes, applications ouvertes sur plusieurs postes avec « Fermer partout », et, pour
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
  /** Fermer cette application sur tous les postes de la salle. */
  killEverywhere: [name: string];
  /** Ouvrir l'assistant de masterisation des postes simulés. */
  reimage: [];
}>();

//------------------ COMPUTED --------------------------------
const figures = computed(() => roomFigures(props.views));
const shared = computed(() => sharedApps(props.views));
const hasShared = computed(() => shared.value.length > 0);
const showReimage = computed(() => props.canReimage && props.views.some(view => view.workstation.simulated));
</script>

<template>
  <section class="room-summary" aria-label="Résumé de la salle">
    <dl class="summary-figures">
      <div><dt>Postes</dt><dd><AnimatedNumber :value="figures.stations" /></dd></div>
      <div><dt>En ligne</dt><dd><AnimatedNumber :value="figures.online" /></dd></div>
      <div><dt>Applications ouvertes</dt><dd><AnimatedNumber :value="figures.openApps" /></dd></div>
    </dl>
    <p v-if="showReimage" class="summary-actions">
      <button
          type="button"
          class="secondary-button"
          :disabled="busy"
          @click="emit('reimage')"
      >
        Masteriser des postes simulés…
      </button>
    </p>
    <div v-if="hasShared" class="shared-apps">
      <h3 class="eyebrow">Ouvertes sur plusieurs postes</h3>
      <ul>
        <li v-for="app in shared" :key="app.name">
          <AppGlyph :name="app.name" />
          <span class="shared-app-text">
            <strong>{{ app.name }}</strong>
            <span class="muted">{{ app.stations }} postes</span>
          </span>
          <button
              type="button"
              class="secondary-button"
              :disabled="busy"
              @click="emit('killEverywhere', app.name)"
          >
            Fermer partout
          </button>
        </li>
      </ul>
    </div>
  </section>
</template>
