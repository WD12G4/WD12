<script setup lang="ts">
/**
 * Affiche l'état d'un chargement : « Chargement… » ou un message d'erreur avec un bouton Réessayer.
 * N'affiche rien quand le chargement a réussi : la vue affiche alors ses données.
 * Utilisé par : les vues qui chargent des données.
 */

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Vrai pendant le chargement. */
  loading: boolean;
  /** Message d'erreur à afficher, ou null. */
  errorMessage: string | null;
}>();

const emit = defineEmits<{
  /** L'utilisateur demande à recharger les données. */
  retry: [];
}>();
</script>

<template>
  <p
      v-if="loading"
      class="muted"
      role="status"
  >
    Chargement…
  </p>
  <div
      v-else-if="errorMessage"
      class="error-box"
      role="alert"
  >
    <p>{{ errorMessage }}</p>
    <button
        type="button"
        class="secondary-button"
        @click="emit('retry')"
    >
      Réessayer
    </button>
  </div>
</template>
