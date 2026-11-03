<script setup lang="ts">
/**
 * Message d'erreur renvoyé par l'API, affiché sous un formulaire (400 : règle métier, 403…).
 * Utilisé par : tous les composants formulaire.
 */

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Message à afficher tel quel (les messages du backend sont déjà en français), ou null. */
  message: string | null;
}>();
</script>

<template>
  <p
      v-if="message"
      class="form-error"
      role="alert"
  >
    {{ message }}
  </p>
</template>
