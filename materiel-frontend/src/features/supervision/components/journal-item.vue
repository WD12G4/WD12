<script setup lang="ts">
/**
 * Un événement du journal, façon notification iOS : icône, quoi, sur quel poste, à quelle heure.
 * Composant de présentation.
 * Utilisé par : supervision-journal.vue
 */
import {computed} from 'vue';
import type {SupervisionEvent} from '../../../models/supervision.model.ts';
import {formatTime} from '../../../shared/composables/use-dates.ts';
import AppGlyph from './app-glyph.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Événement affiché. */
  event: SupervisionEvent;
}>();

//------------------ COMPUTED --------------------------------
/** Application arrêtée, ou « Masterisation » (icône dédiée dans use-app-style.ts). */
const title = computed(() => props.event.processName ?? 'Masterisation');
/** Action et poste : « Application arrêtée sur … », « Arrêt refusé sur … », « Lancée sur … · Ubuntu… ». */
const detail = computed(() => {
  const action = `${actionLabel(props.event)} sur ${props.event.workstationName}`;
  return props.event.detail ? `${action} · ${props.event.detail}` : action;
});
const time = computed(() => formatTime(props.event.at));
const isoTime = computed(() => props.event.at.toISOString());

//------------------ FUNCTIONS -------------------------------
/** Nature de l'action : arrêt réussi ou refusé par l'agent, ou masterisation lancée. */
function actionLabel(event: SupervisionEvent): string {
  if (event.kind === 'reimage') return 'Lancée';
  return event.succeeded ? 'Application arrêtée' : 'Arrêt refusé';
}
</script>

<template>
  <AppGlyph :name="title" />
  <span class="journal-text">
    <strong>{{ title }}</strong>
    <span class="muted">{{ detail }}</span>
  </span>
  <span v-if="event.simulated" class="sim-badge">Simulé</span>
  <span v-if="!event.succeeded" class="badge expiring">Échec</span>
  <time class="journal-time" :datetime="isoTime">{{ time }}</time>
</template>
