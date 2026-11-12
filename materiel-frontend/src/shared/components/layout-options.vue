<script setup lang="ts">
/**
 * Choix de la disposition du menu : barre du haut ou barre latérale, chacune avec un petit schéma.
 * Utilisé par : theme-switch.vue (panneau d'apparence)
 */
import {useTheme} from '../composables/use-theme.ts';

//------------------ DATAS -----------------------------------
const {layouts, currentLayout, setLayout} = useTheme();
</script>

<template>
  <div
      class="layout-options"
      role="radiogroup"
      aria-label="Disposition du menu"
  >
    <button
        v-for="layout in layouts"
        :key="layout.id"
        type="button"
        role="radio"
        class="layout-option"
        :aria-checked="layout.id === currentLayout"
        @click="setLayout(layout.id)"
    >
      <span
          class="layout-preview"
          :class="layout.id"
          aria-hidden="true"
      >
        <span />
      </span>
      {{ layout.label }}
    </button>
  </div>
</template>
