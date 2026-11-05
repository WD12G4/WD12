<script setup lang="ts">
/**
 * Données lues dans le code, repliées par défaut : utiles pour vérifier une lecture douteuse.
 * Utilisé par : scan-result-sheet.vue
 */
import {materialTypeLabels} from '../../../models/material.model.ts';
import type {ScanReading} from '../../../models/scan.model.ts';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  reading: ScanReading;
}>();

/** Libellés de l'origine de la lecture. */
const sourceLabels = {camera: 'caméra', photo: 'photo', manual: 'saisie'} as const;
</script>

<template>
  <details class="reading-details">
    <summary class="mono small">DONNÉES LUES ({{ sourceLabels[reading.source] }})</summary>
    <dl class="facts">
      <div v-if="reading.nsn">
        <dt>N° OTAN</dt>
        <dd class="mono">
          {{ reading.nsn }}
          <span v-if="reading.typeHint" class="muted">→ {{ materialTypeLabels[reading.typeHint] }} ?</span>
        </dd>
      </div>
      <div v-if="reading.macAddress">
        <dt>MAC probable</dt>
        <dd class="mono">{{ reading.macAddress }}</dd>
      </div>
    </dl>
    <pre class="raw">{{ reading.raw }}</pre>
  </details>
</template>
