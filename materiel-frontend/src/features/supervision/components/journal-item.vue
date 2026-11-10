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
const detail = computed(() => `${props.event.kind === 'kill' ? 'Application arrêtée' : 'Lancée'} sur ${props.event.workstationName}`);
const time = computed(() => formatTime(props.event.at));
const isoTime = computed(() => props.event.at.toISOString());
</script>

<template>
  <AppGlyph :name="title" />
  <span class="journal-text">
    <strong>{{ title }}</strong>
    <span class="muted">{{ detail }}</span>
  </span>
  <time class="journal-time" :datetime="isoTime">{{ time }}</time>
</template>
