<script setup lang="ts">
/**
 * Une ligne du tableau des remontées. Un clic sur la ligne (ou sur sa date, au clavier) ouvre le
 * détail dans le panneau latéral ; le document et « Marquer comme traitée » restent en bout de ligne.
 * Composant de présentation.
 * Utilisé par : report-list.vue
 * RG : RG-22 (document disponible), RG-23 (à traiter / traitée)
 */
import {computed} from 'vue';
import {problemTypeLabels, type Report} from '../../../models/report.model.ts';
import {formatDateTime} from '../../../shared/composables/use-dates.ts';
import {locationLabel} from '../../../shared/composables/use-room-label.ts';
import Link from '../../../shared/components/Link.vue';
import ReportStatusBadge from './report-status-badge.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Remontée affichée. */
  report: Report;
  /** Vrai si la colonne « Signalé par » est affichée (gestionnaire, administrateur). */
  showAuthor: boolean;
  /** Vrai si le bouton « Marquer comme traitée » est affiché (gestionnaire, administrateur). */
  canHandle: boolean;
}>();

const emit = defineEmits<{
  /** Ouverture du détail de la remontée. */
  open: [report: Report];
  /** Le gestionnaire marque la remontée comme traitée, ou la rouvre. */
  toggleHandled: [report: Report];
}>();

//------------------ COMPUTED --------------------------------
const createdAt = computed(() => formatDateTime(props.report.createdAt));
const location = computed(() => locationLabel(props.report.material.room));
const problem = computed(() => (props.report.problemType ? problemTypeLabels[props.report.problemType] : '—'));
const toggleLabel = computed(() => (props.report.handled ? 'Rouvrir' : 'Marquer comme traitée'));
const openLabel = computed(() => `Voir le détail de la remontée du ${createdAt.value}`);
</script>

<template>
  <td>
    <button
        type="button"
        class="row-link"
        :aria-label="openLabel"
        @click="emit('open', report)"
    >
      {{ createdAt }}
    </button>
  </td>
  <td>{{ location }}</td>
  <td class="code">{{ report.material.serialNumber }}</td>
  <td>{{ problem }}</td>
  <td v-if="showAuthor">{{ report.author }}</td>
  <td><ReportStatusBadge :handled="report.handled" /></td>
  <td class="row-actions">
    <Link :to="{ name: 'report-document', params: { id: report.id } }">Document</Link>
    <button
        v-if="canHandle"
        type="button"
        class="secondary-button"
        @click="emit('toggleHandled', report)"
    >
      {{ toggleLabel }}
    </button>
  </td>
</template>
