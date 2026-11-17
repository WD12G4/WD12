<script setup lang="ts">
/**
 * Pastille de l'état d'une remontée : « À traiter » ou « Traitée ».
 * Composant de présentation.
 * Utilisé par : report-item.vue, report-panel.vue
 * RG : RG-36 (état d'une remontée), RG-37 (le formateur voit que sa remontée est traitée)
 */

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Vrai si le gestionnaire a traité la remontée. */
  handled: boolean;
}>();
</script>

<template>
  <span v-if="handled" class="badge archived">Traitée</span>
  <span v-else class="badge expiring">À traiter</span>
</template>
