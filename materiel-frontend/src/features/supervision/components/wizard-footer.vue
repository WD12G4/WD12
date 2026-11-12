<script setup lang="ts">
/**
 * Boutons de l'assistant de masterisation : Retour (sauf à la première étape), Suivant, puis
 * « Lancer la masterisation » à la dernière étape (désactivé sans poste coché).
 * Composant de présentation.
 * Utilisé par : reimage-wizard.vue
 */

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Vrai à partir de la deuxième étape. */
  canGoBack: boolean;
  /** Vrai à la dernière étape. */
  isLast: boolean;
  /** Vrai si au moins un poste est coché. */
  canLaunch: boolean;
}>();

const emit = defineEmits<{
  back: [];
  next: [];
  launch: [];
}>();
</script>

<template>
  <footer class="actions wizard-actions">
    <button
        v-if="canGoBack"
        type="button"
        class="secondary-button"
        @click="emit('back')"
    >
      Retour
    </button>
    <button
        v-if="!isLast"
        type="button"
        @click="emit('next')"
    >
      Suivant
    </button>
    <button
        v-else
        type="button"
        :disabled="!canLaunch"
        @click="emit('launch')"
    >
      Lancer la masterisation
    </button>
  </footer>
</template>
