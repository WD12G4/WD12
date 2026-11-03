<script setup lang="ts">
/**
 * Carte de mise en page avec un titre et trois emplacements (header, body, footer).
 * Copié du projet du cours (vue1) : nom de fichier gardé tel quel.
 * Utilisé par : toutes les vues.
 */

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Titre affiché en haut de la carte. */
  title: string;
}>();
</script>

<template>
  <section class="card">
    <h2>{{ title }}</h2>
    <slot name="header" />
    <slot name="body" />
    <slot name="footer" />
  </section>
</template>

<style scoped>
.card {
  margin-bottom: 1.5rem;
  padding: 1.5rem;
  border: 1px solid var(--border);
  border-radius: 1.25rem;
}
</style>
