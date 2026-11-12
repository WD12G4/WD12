<script setup lang="ts">
/**
 * Dernière étape de l'assistant de masterisation : postes simulés à masteriser (cases à cocher),
 * récapitulatif de la configuration, durée annoncée et avertissement.
 * Composant de présentation.
 * Utilisé par : reimage-wizard.vue
 * RG : RG-28
 */
import type {WorkstationView} from '../composables/use-supervision.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Postes simulés de la salle. */
  stations: WorkstationView[];
  /** Identifiants des postes cochés. */
  selected: number[];
  /** Configuration en clair (« Ubuntu 24.04 LTS · Développement · 7 applications »). */
  summary: string;
  /** Durée annoncée (« environ 35 s »). */
  estimate: string;
}>();

const emit = defineEmits<{
  /** Un poste est coché ou décoché. */
  toggle: [workstationId: number];
}>();

//------------------ FUNCTIONS -------------------------------
const isChecked = (view: WorkstationView): boolean => props.selected.includes(view.workstation.id);
/** Un poste déjà en cours de masterisation ne peut pas être coché. */
const isBusy = (view: WorkstationView): boolean => view.workstation.status === 'reimaging';
</script>

<template>
  <fieldset class="target-list">
    <legend class="eyebrow">Postes à masteriser</legend>
    <label v-for="view in stations" :key="view.workstation.id">
      <input
          type="checkbox"
          :checked="isChecked(view)"
          :disabled="isBusy(view)"
          @change="emit('toggle', view.workstation.id)"
      >
      <span>{{ view.workstation.name }}</span>
      <span v-if="view.installed" class="muted">{{ view.installed.system }} · {{ view.installed.image }}</span>
    </label>
  </fieldset>
  <dl class="detail-grid">
    <div class="full-width"><dt>Nouvelle configuration</dt><dd>{{ summary }}</dd></div>
    <div><dt>Postes</dt><dd>{{ selected.length }}</dd></div>
    <div><dt>Durée</dt><dd>{{ estimate }}</dd></div>
  </dl>
  <p class="field-hint">Simulation : les postes sont réinstallés, le travail non enregistré est perdu.</p>
</template>
