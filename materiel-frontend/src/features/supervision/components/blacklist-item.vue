<script setup lang="ts">
/**
 * Une application du catalogue dans l'onglet « Liste noire » : icône, nom, état, bouton Interdire ou
 * Autoriser. Composant de présentation.
 * Utilisé par : blacklist-panel.vue
 * RG : RG-27
 */
import {computed} from 'vue';
import type {Application} from '../../../models/application.model.ts';
import AppGlyph from './app-glyph.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Application affichée. */
  app: Application;
  /** Vrai pendant une action. */
  busy: boolean;
}>();

const emit = defineEmits<{
  /** Interdire (true) ou autoriser (false) l'application. */
  toggle: [app: Application, blacklisted: boolean];
}>();

//------------------ COMPUTED --------------------------------
/** Nom du processus, affiché sous le nom lisible s'il est différent. */
const processName = computed(() => (props.app.name !== props.app.displayName ? props.app.name : ''));
const actionLabel = computed(() => (props.app.blacklisted ? 'Autoriser' : 'Interdire'));
/** Autoriser : bouton secondaire ; interdire : bouton rouge. */
const actionClass = computed(() => (props.app.blacklisted ? 'secondary-button' : 'delete-button'));
</script>

<template>
  <AppGlyph :name="app.name" :icon="app.icon" />
  <span class="process-text">
    <strong>{{ app.displayName }}</strong>
    <span v-if="processName" class="muted">{{ processName }}</span>
  </span>
  <span v-if="app.blacklisted" class="badge expiring">Interdite</span>
  <button
      type="button"
      :class="actionClass"
      :disabled="busy"
      @click="emit('toggle', app, !app.blacklisted)"
  >
    {{ actionLabel }}
  </button>
</template>
