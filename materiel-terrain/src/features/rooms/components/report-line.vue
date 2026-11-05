<script setup lang="ts">
/**
 * Un matériel du compte rendu : numéro, type et situation ; un appui déplie les informations connues.
 * Consultation seule : aucune action ici (les corrections se font depuis l'écran Scanner).
 * Utilisé par : room-view.vue, rooms-view.vue
 */
import {computed} from 'vue';
import {materialTypeLabels} from '../../../models/material.model.ts';
import {formatDate, formatTime} from '../../../shared/composables/use-dates.ts';
import MaterialFacts from '../../scan/components/material-facts.vue';
import type {LineStatus, ReportLine} from '../lib/room-report.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  line: ReportLine;
  /** Texte sous les informations : où et quand il a été scanné (recherche), sinon la date du scan ici. */
  seenLabel?: string;
}>();

//------------------ VALEURS ---------------------------------
/** Badge selon la situation. */
const badges: Record<LineStatus, {kind: string; text: string}> = {
  present: {kind: 'ok', text: 'sur place'},
  missing: {kind: 'warn', text: 'non constaté'},
  elsewhere: {kind: 'warn', text: 'déclaré ailleurs'},
  stock: {kind: 'warn', text: 'déclaré en stock'},
  unknown: {kind: 'danger', text: 'non recensé'},
  archived: {kind: 'danger', text: 'archivé'},
};

//------------------ COMPUTED --------------------------------
const badge = computed(() => badges[props.line.status]);
const seen = computed(() => {
  if (props.seenLabel !== undefined) return props.seenLabel;
  const at = props.line.seenAt;
  return at ? `Scanné ici le ${formatDate(at)} à ${formatTime(at)}.` : 'Pas encore scanné dans cette salle.';
});
</script>

<template>
  <li>
    <details class="report-line">
      <summary>
        <span class="line-id">
          <span class="mono">{{ line.serialNumber }}</span>
          <span v-if="line.material" class="muted small">{{ materialTypeLabels[line.material.materialType] }}</span>
        </span>
        <span class="badge" :class="badge.kind">{{ badge.text }}</span>
      </summary>
      <MaterialFacts v-if="line.material" :material="line.material" />
      <p v-else class="small">Numéro absent du parc : à prendre en compte depuis l'écran Scanner.</p>
      <p v-if="line.material?.archived" class="small">Archivé : il ne peut plus être affecté à une salle.</p>
      <p class="muted small">{{ seen }}</p>
    </details>
  </li>
</template>
