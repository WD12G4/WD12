<script setup lang="ts">
/**
 * Bandeau du message temporaire partagé (use-flash-message.ts).
 * Utilisé par : App.vue
 */
import {useFlashMessage} from '../composables/use-flash-message.ts';

//------------------ DATAS -----------------------------------
const {message, clear} = useFlashMessage();
</script>

<template>
  <div
      v-if="message"
      class="flash"
      :class="message.kind"
      role="status"
  >
    <span>{{ message.text }}</span>
    <button
        type="button"
        class="icon-button"
        aria-label="Fermer le message"
        @click="clear"
    >
      ×
    </button>
  </div>
</template>
