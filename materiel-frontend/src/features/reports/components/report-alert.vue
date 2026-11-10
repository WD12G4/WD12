<script setup lang="ts">
/**
 * Pop-up affichée à la connexion du gestionnaire quand des remontées sont à traiter (RG-23).
 * Composant de présentation : le compteur est calculé par use-unread-reports.ts.
 * Utilisé par : App.vue
 */
import Link from '../../../shared/components/Link.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Nombre de remontées à traiter. */
  count: number;
}>();

const emit = defineEmits<{
  /** L'utilisateur ferme la pop-up. */
  dismiss: [];
}>();
</script>

<template>
  <div class="dialog-backdrop">
    <div
        class="dialog card"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="report-alert-title"
    >
      <h2 id="report-alert-title">Nouvelles remontées</h2>
      <p>{{ count }} remontée(s) de problème attendent votre vérification.</p>
      <div class="actions">
        <Link
            class="button-link"
            :to="{ name: 'reports' }"
            @click="emit('dismiss')"
        >
          Voir les remontées
        </Link>
        <button
            type="button"
            class="secondary-button"
            @click="emit('dismiss')"
        >
          Plus tard
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dialog-backdrop {
  position: fixed;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 1rem;
  background: var(--backdrop);
  z-index: 10;
}

/* Largeur explicite : les cartes ont container-type (style.css), sans largeur elles se réduiraient à rien. */
.dialog {
  width: min(460px, 100%);
  padding: 1.5rem;
  border-radius: 1.25rem;
}
</style>
