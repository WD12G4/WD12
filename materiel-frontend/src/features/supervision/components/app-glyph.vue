<script setup lang="ts">
/**
 * Icône d'application façon iOS (carré arrondi coloré), ou son initiale si elle n'a pas d'icône.
 * Décorative : le nom de l'application est écrit à côté, ou porté par l'aria-label du parent.
 * Utilisé par : station-tile.vue, process-item.vue, room-summary.vue, journal-item.vue
 */
import {computed} from 'vue';
import {appStyle} from '../composables/use-app-style.ts';
import AppIcon from '../../../shared/components/app-icon.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Nom de l'application. */
  name: string;
}>();

//------------------ COMPUTED --------------------------------
const style = computed(() => appStyle(props.name));
/** Couleur de fond (dégradé --app-* de style.css). */
const toneClass = computed(() => `tone-${style.value.tone}`);
</script>

<template>
  <span
      class="app-glyph"
      :class="toneClass"
      aria-hidden="true"
  >
    <AppIcon v-if="style.icon" :name="style.icon" />
    <span v-else>{{ style.letter }}</span>
  </span>
</template>
