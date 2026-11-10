<script setup lang="ts">
/**
 * Une ligne du tableau des remontées.
 * Composant de présentation.
 * Utilisé par : report-list.vue
 * RG : RG-22 (document disponible), RG-23 (à traiter / traitée)
 */
import {computed} from 'vue';
import {problemTypeLabels, type Report} from '../../../models/report.model.ts';
import {formatDateTime} from '../../../shared/composables/use-dates.ts';
import {locationLabel} from '../../../shared/composables/use-room-label.ts';
import Link from '../../../shared/components/Link.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Remontée affichée. */
  report: Report;
}>();

const emit = defineEmits<{
  /** Le gestionnaire marque la remontée comme traitée, ou la rouvre. */
  toggleHandled: [report: Report];
}>();

//------------------ COMPUTED --------------------------------
const createdAt = computed(() => formatDateTime(props.report.createdAt));
const location = computed(() => locationLabel(props.report.material.room));
const problem = computed(() => (props.report.problemType ? problemTypeLabels[props.report.problemType] : '—'));
const toggleLabel = computed(() => (props.report.handled ? 'Rouvrir' : 'Marquer comme traitée'));
</script>

<template>
  <td>{{ createdAt }}</td>
  <td>{{ location }}</td>
  <td class="code">{{ report.material.serialNumber }}</td>
  <td>{{ problem }}</td>
  <td>{{ report.author }}</td>
  <td>
    <span v-if="report.handled" class="badge archived">Traitée</span>
    <span v-else class="badge expiring">À traiter</span>
  </td>
  <td class="row-actions">
    <Link :to="{ name: 'report-document', params: { id: report.id } }">Document</Link>
    <button
        type="button"
        class="secondary-button"
        @click="emit('toggleHandled', report)"
    >
      {{ toggleLabel }}
    </button>
  </td>
</template>
