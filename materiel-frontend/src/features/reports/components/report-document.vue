<script setup lang="ts">
/**
 * Contenu du document d'état des lieux d'une remontée : quel poste, quelle salle, quel jour, quel problème.
 * Conçu pour être imprimé (ou enregistré en PDF depuis la fenêtre d'impression du navigateur).
 * Composant de présentation.
 * Utilisé par : report-document-view.vue
 * RG : RG-22
 */
import {computed} from 'vue';
import {problemTypeLabels, type Report} from '../../../models/report.model.ts';
import {formatDateTime} from '../../../shared/composables/use-dates.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Remontée décrite par le document. */
  report: Report;
}>();

//------------------ COMPUTED --------------------------------
const createdAt = computed(() => formatDateTime(props.report.createdAt));
const material = computed(() => props.report.materialSerialNumber ?? 'Aucun matériel précis');
const description = computed(() => props.report.description || 'Aucune description.');
</script>

<template>
  <article>
    <h3>État des lieux — remontée n° {{ report.id }}</h3>
    <dl class="detail-grid">
      <div>
        <dt>Date</dt>
        <dd>{{ createdAt }}</dd>
      </div>
      <div>
        <dt>Signalé par</dt>
        <dd>{{ report.author }}</dd>
      </div>
      <div>
        <dt>Salle</dt>
        <dd>{{ report.roomLabel }}</dd>
      </div>
      <div>
        <dt>Matériel</dt>
        <dd>{{ material }}</dd>
      </div>
      <div>
        <dt>Problème</dt>
        <dd>{{ problemTypeLabels[report.problemType] }}</dd>
      </div>
    </dl>
    <h3>Description</h3>
    <p>{{ description }}</p>
  </article>
</template>
