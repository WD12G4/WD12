<script setup lang="ts">
/**
 * Détail d'une remontée dans le panneau latéral : date, auteur, salle, matériel, problème, état et
 * description complète, sans ouvrir le document. Actions : document d'état des lieux et, pour le
 * gestionnaire, « Marquer comme traitée » ou « Rouvrir ».
 * Composant de présentation.
 * Utilisé par : report-view.vue
 * RG : RG-21 (contenu), RG-22 (document), RG-23 (traitement)
 */
import {computed} from 'vue';
import {problemTypeLabels, type Report} from '../../../models/report.model.ts';
import {materialTypeLabels} from '../../../models/material.model.ts';
import {formatDateTime} from '../../../shared/composables/use-dates.ts';
import {locationLabel} from '../../../shared/composables/use-room-label.ts';
import Link from '../../../shared/components/Link.vue';
import SidePanel from '../../../shared/components/side-panel.vue';
import ReportStatusBadge from './report-status-badge.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Remontée affichée, ou null quand le panneau est fermé. */
  report: Report | null;
  /** Vrai si l'auteur est affiché (gestionnaire, administrateur). */
  showAuthor: boolean;
  /** Vrai si l'utilisateur peut marquer la remontée comme traitée (gestionnaire, administrateur). */
  canHandle: boolean;
}>();

const emit = defineEmits<{
  /** Fermeture du panneau. */
  close: [];
  /** La remontée est marquée comme traitée, ou rouverte. */
  toggleHandled: [report: Report];
}>();

//------------------ COMPUTED --------------------------------
const title = computed(() => (props.report?.problemType ? problemTypeLabels[props.report.problemType] : 'Remontée'));
const createdAt = computed(() => (props.report ? formatDateTime(props.report.createdAt) : ''));
const location = computed(() => (props.report ? locationLabel(props.report.material.room) : ''));
const materialType = computed(() => (props.report ? materialTypeLabels[props.report.material.materialType] : ''));
const handledAt = computed(() => (props.report?.handledAt ? `le ${formatDateTime(props.report.handledAt)}` : ''));
const description = computed(() => props.report?.description || 'Aucune description.');
const toggleLabel = computed(() => (props.report?.handled ? 'Rouvrir' : 'Marquer comme traitée'));
</script>

<template>
  <SidePanel
      :panel-key="report?.id ?? null"
      :title="title"
      eyebrow="Remontée"
      @close="emit('close')"
  >
    <dl v-if="report" class="detail-grid">
      <div class="full-width">
        <dt>Description</dt>
        <dd class="report-description">{{ description }}</dd>
      </div>
      <div>
        <dt>Signalée le</dt>
        <dd>{{ createdAt }}</dd>
      </div>
      <div v-if="showAuthor">
        <dt>Signalée par</dt>
        <dd>{{ report.author }}</dd>
      </div>
      <div>
        <dt>Salle</dt>
        <dd>{{ location }}</dd>
      </div>
      <div>
        <dt>Matériel</dt>
        <dd><span class="code">{{ report.material.serialNumber }}</span> · {{ materialType }}</dd>
      </div>
      <div>
        <dt>État</dt>
        <dd><ReportStatusBadge :handled="report.handled" /> {{ handledAt }}</dd>
      </div>
    </dl>
    <template v-if="report" #actions>
      <Link class="button-link" :to="{ name: 'report-document', params: { id: report.id } }">Document</Link>
      <button
          v-if="canHandle"
          type="button"
          class="secondary-button"
          @click="emit('toggleHandled', report)"
      >
        {{ toggleLabel }}
      </button>
    </template>
  </SidePanel>
</template>
