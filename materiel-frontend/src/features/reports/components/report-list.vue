<script setup lang="ts">
/**
 * Tableau des remontées, de la plus récente à la plus ancienne.
 * Composant de présentation.
 * Utilisé par : report-view.vue
 */
import type {Report} from '../../../models/report.model.ts';
import ReportItem from './report-item.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Remontées à afficher. */
  reports: Report[];
}>();

const emit = defineEmits<{
  /** Une remontée est marquée comme traitée, ou rouverte. */
  toggleHandled: [report: Report];
}>();
</script>

<template>
  <p v-if="reports.length === 0" class="empty-state">Aucune remontée pour le moment.</p>
  <table v-else class="data-table">
    <thead>
      <tr>
        <th scope="col">Date</th>
        <th scope="col">Salle</th>
        <th scope="col">Matériel</th>
        <th scope="col">Problème</th>
        <th scope="col">Signalé par</th>
        <th scope="col">État</th>
        <th scope="col"><span class="visually-hidden">Actions</span></th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="report in reports" :key="report.id">
        <ReportItem :report="report" @toggle-handled="emit('toggleHandled', $event)" />
      </tr>
    </tbody>
  </table>
</template>
