<script setup lang="ts">
/**
 * Une application ouverte sur un poste (panneau de détail) : icône, nom, depuis quand, bouton Arrêter,
 * et, pour l'administrateur sur un vrai poste, bouton Interdire (liste noire).
 * Composant de présentation.
 * Utilisé par : process-list.vue
 * RG : RG-26 (arrêt en un clic), RG-27 (liste noire)
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
  /** Vrai si le bouton Interdire est affiché (administrateur, vrai poste). */
  canForbid: boolean;
}>();

const emit = defineEmits<{
  /** Demande d'arrêt de l'application. */
  kill: [process: ProcessInfo];
  /** Demande d'interdiction de l'application (liste noire). */
  forbid: [process: ProcessInfo];
}>();

//------------------ COMPUTED --------------------------------
const elapsed = computed(() => formatElapsed(props.process.startedAt));
/** Date exacte, en info-bulle de la durée. */
const startedAt = computed(() => `Lancée le ${formatDateTime(props.process.startedAt)}`);
</script>

<template>
  <AppGlyph :name="process.name" :icon="process.icon" />
  <span class="process-text">
    <strong>{{ process.name }}</strong>
    <span class="muted" :title="startedAt">{{ elapsed }}</span>
  </span>
  <span class="process-actions">
    <button
        v-if="canForbid"
        type="button"
        class="secondary-button"
        :disabled="busy"
        title="Fermer cette application sur tous les postes, maintenant et à chaque lancement"
        @click="emit('forbid', process)"
    >
      Interdire
    </button>
    <button
        type="button"
        class="delete-button"
        :disabled="busy"
        @click="emit('kill', process)"
    >
      Arrêter
    </button>
  </span>
</template>
