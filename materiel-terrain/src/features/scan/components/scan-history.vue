<script setup lang="ts">
/**
 * Dernières lectures de la session, avec l'action menée.
 * Utilisé par : features/scan/views/scan-view.vue
 */
import {verdictLabels} from '../../../models/scan.model.ts';
import {formatTime} from '../../../shared/composables/use-dates.ts';
import type {HistoryEntry} from '../composables/use-scan-history.ts';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  entries: HistoryEntry[];
}>();
</script>

<template>
  <section v-if="entries.length > 0" class="history">
    <h2 class="mono small">DERNIÈRES LECTURES</h2>
    <ul class="plain-list">
      <li v-for="entry in entries" :key="entry.id" class="history-item">
        <span class="mono">{{ entry.serialNumber }}</span>
        <span class="history-note" :class="entry.note ? 'done' : `verdict-${entry.verdict}`">
          {{ entry.note ?? verdictLabels[entry.verdict] }}
        </span>
        <time class="muted small">{{ formatTime(entry.at) }}</time>
      </li>
    </ul>
  </section>
</template>
