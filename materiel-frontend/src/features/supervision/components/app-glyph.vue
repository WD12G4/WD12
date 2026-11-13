<script setup lang="ts">
/**
 * Icône d'application façon iOS (carré arrondi) : la vraie icône envoyée par l'agent (postes Windows)
 * si elle existe, sinon un pictogramme coloré ou l'initiale de l'application.
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
  /** Icône envoyée par l'agent (image en data URI déjà vérifiée), ou null. */
  icon?: string | null;
}>();

//------------------ COMPUTED --------------------------------
const style = computed(() => appStyle(props.name));
/** Couleur de fond (dégradé --app-* de style.css), ou fond neutre derrière une vraie icône. */
const toneClass = computed(() => (props.icon ? 'has-image' : `tone-${style.value.tone}`));
</script>

<template>
  <span
      class="app-glyph"
      :class="toneClass"
      aria-hidden="true"
  >
    <img
        v-if="icon"
        :src="icon"
        alt=""
    >
    <AppIcon v-else-if="style.icon" :name="style.icon" />
    <span v-else>{{ style.letter }}</span>
  </span>
</template>
