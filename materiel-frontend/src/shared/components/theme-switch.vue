<script setup lang="ts">
/**
 * Choix de l'apparence : thème actuel (« Classique ») ou essai « Tactique » (use-theme.ts),
 * pour comparer les deux avant de proposer le second au client.
 * Utilisé par : app-nav.vue (barre latérale)
 */
import {type ThemeName, useTheme} from '../composables/use-theme.ts';
import type {SelectOption} from '../composables/use-listbox.ts';
import SegmentedControl from './segmented-control.vue';

//------------------ DATAS -----------------------------------
const {theme} = useTheme();
const themeOptions: SelectOption<ThemeName>[] = [
  {value: 'classique', label: 'Classique'},
  {value: 'tactique', label: 'Tactique'},
];
</script>

<template>
  <div class="theme-switch">
    <span id="theme-label" class="field-label">Apparence</span>
    <SegmentedControl
        v-model="theme"
        :options="themeOptions"
        labelledby="theme-label"
    />
  </div>
</template>
