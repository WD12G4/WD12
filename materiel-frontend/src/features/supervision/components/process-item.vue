<script setup lang="ts">
/**
 * Une application ouverte sur un poste (panneau de détail) : icône, nom, depuis quand, bouton Arrêter.
 * Composant de présentation.
 * Utilisé par : process-list.vue
 * RG : RG-26 (arrêt en un clic)
 */
import {computed} from 'vue';
import type {ProcessInfo} from '../../../models/supervision.model.ts';
import {formatDateTime, formatElapsed} from '../../../shared/composables/use-dates.ts';
import AppGlyph from './app-glyph.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Application affichée. */
  process: ProcessInfo;
  /** Vrai pendant une action, pour éviter les doubles clics. */
  busy: boolean;
}>();

const emit = defineEmits<{
  /** Demande d'arrêt de l'application. */
  kill: [process: ProcessInfo];
}>();

//------------------ COMPUTED --------------------------------
const elapsed = computed(() => formatElapsed(props.process.startedAt));
/** Date exacte, en info-bulle de la durée. */
const startedAt = computed(() => `Lancée le ${formatDateTime(props.process.startedAt)}`);
</script>

<template>
  <AppGlyph :name="process.name" />
  <span class="process-text">
    <strong>{{ process.name }}</strong>
    <span class="muted" :title="startedAt">{{ elapsed }}</span>
  </span>
  <button
      type="button"
      class="delete-button"
      :disabled="busy"
      @click="emit('kill', process)"
  >
    Arrêter
  </button>
</template>
