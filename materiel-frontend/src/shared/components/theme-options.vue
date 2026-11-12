<script setup lang="ts">
/**
 * Liste des thèmes du panneau d'apparence. Chaque thème a un aperçu vivant : une vignette qui porte
 * son data-theme, donc ses vraies couleurs (fond à reflets, plaque de verre, pastille d'accent).
 * Utilisé par : theme-switch.vue
 */
import {useTheme} from '../composables/use-theme.ts';

//------------------ DATAS -----------------------------------
const {themes, currentTheme, setTheme} = useTheme();
</script>

<template>
  <div
      class="theme-options"
      role="radiogroup"
      aria-label="Thème"
  >
    <button
        v-for="theme in themes"
        :key="theme.id"
        type="button"
        role="radio"
        class="theme-option"
        :aria-checked="theme.id === currentTheme"
        @click="setTheme(theme.id)"
    >
      <span class="theme-preview" :data-theme="theme.id">
        <span class="theme-preview-glass"><span class="theme-preview-dot" /></span>
      </span>
      <span class="theme-option-text">
        <strong>{{ theme.label }}</strong>
        <span>{{ theme.description }}</span>
      </span>
    </button>
  </div>
</template>
