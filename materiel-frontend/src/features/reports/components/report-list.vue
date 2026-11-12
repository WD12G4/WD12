<script setup lang="ts">
/**
 * Tableau des remontées, de la plus récente à la plus ancienne. Un clic sur une ligne ouvre son
 * détail (les liens et boutons de la ligne gardent leur propre action).
 * Composant de présentation.
 * Utilisé par : report-view.vue
 */
import type {Report} from '../../../models/report.model.ts';
import ReportItem from './report-item.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Remontées à afficher. */
  reports: Report[];
  /** Identifiant de la remontée ouverte dans le panneau, ou null. */
  selectedId: number | null;
  /** Vrai si la colonne « Signalé par » est affichée (gestionnaire, administrateur). */
  showAuthor: boolean;
  /** Vrai si les remontées peuvent être marquées comme traitées (gestionnaire, administrateur). */
  canHandle: boolean;
}>();

const emit = defineEmits<{
  /** Ouverture du détail d'une remontée. */
  open: [report: Report];
  /** Une remontée est marquée comme traitée, ou rouverte. */
  toggleHandled: [report: Report];
}>();

//------------------ FUNCTIONS -------------------------------
const rowClass = (report: Report): Record<string, boolean> => ({previewed: report.id === props.selectedId});

/**
 * Clic sur une ligne : ouvre le détail, sauf sur un lien ou un bouton (Document, Marquer comme traitée).
 * @param event clic reçu par la ligne
 * @param report remontée de la ligne
 */
function onRowClick(event: MouseEvent, report: Report): void {
  if (event.target instanceof Element && event.target.closest('a, button')) return;
  emit('open', report);
}
</script>

<template>
  <p v-if="reports.length === 0" class="empty-state">Aucune remontée pour le moment.</p>
  <table v-else class="data-table clickable-rows">
    <thead>
      <tr>
        <th scope="col">Date</th>
        <th scope="col">Salle</th>
        <th scope="col">Matériel</th>
        <th scope="col">Problème</th>
        <th v-if="showAuthor" scope="col">Signalé par</th>
        <th scope="col">État</th>
        <th scope="col"><span class="visually-hidden">Actions</span></th>
      </tr>
    </thead>
    <tbody>
      <tr
          v-for="report in reports"
          :key="report.id"
          :class="rowClass(report)"
          @click="onRowClick($event, report)"
      >
        <ReportItem
            :report="report"
            :show-author="showAuthor"
            :can-handle="canHandle"
            @open="emit('open', $event)"
            @toggle-handled="emit('toggleHandled', $event)"
        />
      </tr>
    </tbody>
  </table>
</template>
