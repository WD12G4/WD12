<script setup lang="ts">
/**
 * Choix de l'apparence : thème « Classique » (barre latérale) ou essai « Verre » (barre du haut,
 * verre dépoli), pour comparer les deux. Pastille flottante en bas à droite, sur toutes les pages.
 * Utilisé par : App.vue
 */
import {type ThemeName, useTheme} from '../composables/use-theme.ts';
import type {SelectOption} from '../composables/use-listbox.ts';
import SegmentedControl from './segmented-control.vue';

//------------------ DATAS -----------------------------------
const {theme} = useTheme();
const themeOptions: SelectOption<ThemeName>[] = [
  {value: 'classique', label: 'Classique'},
  {value: 'verre', label: 'Verre'},
];
</script>

<template>
  <div class="theme-switch no-print">
    <span id="theme-label" class="field-label">Apparence</span>
    <SegmentedControl
        v-model="theme"
        :options="themeOptions"
        labelledby="theme-label"
    />
  </div>
</template>
