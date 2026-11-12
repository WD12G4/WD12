<script setup lang="ts">
/**
 * Étape « Applications » de l'assistant de masterisation : les applications disponibles sur le
 * système choisi, en icônes à cocher (celles de l'image de base sont déjà cochées).
 * Composant de présentation.
 * Utilisé par : reimage-wizard.vue
 * RG : RG-28
 */
import {computed} from 'vue';
import type {AppOption} from '../../../models/reimage.model.ts';
import {processName} from '../../../services/simulation-catalog.mock.ts';
import AppGlyph from './app-glyph.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Applications proposées (disponibles sur le système choisi). */
  apps: AppOption[];
  /** Identifiants des applications cochées. */
  selected: string[];
  /** Système choisi : donne le nom du processus, donc l'icône. */
  osId: string;
}>();

const emit = defineEmits<{
  /** Une application est cochée ou décochée. */
  toggle: [appId: string];
}>();

//------------------ COMPUTED --------------------------------
const items = computed(() => props.apps.map(app => ({
  ...app, glyph: processName(app.id, props.osId) ?? app.label, checked: props.selected.includes(app.id),
})));
const countLabel = computed(() => `${props.selected.length} application${props.selected.length > 1 ? 's' : ''} sélectionnée${props.selected.length > 1 ? 's' : ''}`);
</script>

<template>
  <p class="muted">{{ countLabel }}</p>
  <ul class="app-picker">
    <li v-for="app in items" :key="app.id">
      <button
          type="button"
          class="app-option"
          :aria-pressed="app.checked"
          @click="emit('toggle', app.id)"
      >
        <AppGlyph :name="app.glyph" />
        <span>{{ app.label }}</span>
      </button>
    </li>
  </ul>
</template>
