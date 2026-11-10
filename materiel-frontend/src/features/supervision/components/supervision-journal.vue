<script setup lang="ts">
/**
 * Journal de la salle (entretien 4 : quel poste, à quelle heure) : applications arrêtées et
 * masterisations, regroupées par jour, de la plus récente à la plus ancienne.
 * Composant de présentation.
 * Utilisé par : supervision-view.vue
 */
import {computed} from 'vue';
import type {SupervisionEvent} from '../../../models/supervision.model.ts';
import {groupByDay} from '../composables/use-room-activity.ts';
import JournalItem from './journal-item.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Événements de la salle, du plus récent au plus ancien. */
  events: SupervisionEvent[];
}>();

//------------------ COMPUTED --------------------------------
const days = computed(() => groupByDay(props.events));
</script>

<template>
  <p v-if="days.length === 0" class="empty-state">Aucune action enregistrée pour cette salle.</p>
  <section
      v-for="day in days"
      :key="day.label"
      class="journal-day"
  >
    <h3 class="eyebrow">{{ day.label }}</h3>
    <ol class="journal">
      <li
          v-for="event in day.events"
          :key="event.id"
          class="journal-item"
      >
        <JournalItem :event="event" />
      </li>
    </ol>
  </section>
</template>
