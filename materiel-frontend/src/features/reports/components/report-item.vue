<script setup lang="ts">
/**
 * Une ligne du tableau des remontées.
 * Composant de présentation.
 * Utilisé par : report-list.vue
 * RG : RG-22 (document disponible), RG-23 (lue / non lue)
 */
import {computed} from 'vue';
import {problemTypeLabels, type Report} from '../../../models/report.model.ts';
import {formatDateTime} from '../../../shared/composables/use-dates.ts';
import Link from '../../../shared/components/Link.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Remontée affichée. */
  report: Report;
}>();

const emit = defineEmits<{
  /** Le gestionnaire indique avoir traité la remontée. */
  markAsRead: [report: Report];
}>();

//------------------ COMPUTED --------------------------------
const createdAt = computed(() => formatDateTime(props.report.createdAt));
const material = computed(() => props.report.materialSerialNumber ?? '—');
</script>

<template>
  <td>{{ createdAt }}</td>
  <td>{{ report.roomLabel }}</td>
  <td>{{ material }}</td>
  <td>{{ problemTypeLabels[report.problemType] }}</td>
  <td>
    <span v-if="report.read" class="badge archived">Lue</span>
    <span v-else class="badge expiring">Non lue</span>
  </td>
  <td class="row-actions">
    <Link :to="{ name: 'report-document', params: { id: report.id } }">Document</Link>
    <button
        v-if="!report.read"
        type="button"
        class="secondary-button"
        @click="emit('markAsRead', report)"
    >
      Marquer comme lue
    </button>
  </td>
</template>
