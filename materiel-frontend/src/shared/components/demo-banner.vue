<script setup lang="ts">
/**
 * Bandeau signalant un écran en partie simulé (données de démonstration).
 * Le message par défaut peut être remplacé par le slot par défaut, pour préciser ce qui est simulé.
 * Utilisé par : supervision-view.vue (applications ouvertes simulées en attendant l'agent).
 */
</script>

<template>
  <p class="demo-banner" role="note">
    <slot>Données de démonstration : cet écran n'est pas encore relié au serveur.</slot>
  </p>
</template>
