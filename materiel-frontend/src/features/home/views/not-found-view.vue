<script setup lang="ts">
/**
 * Page affichée pour une adresse inconnue.
 */
import Card from '../../../shared/components/card.vue';
import Link from '../../../shared/components/Link.vue';
</script>

<template>
  <Card title="Page introuvable">
    <template #body>
      <p>Cette page n'existe pas.</p>
      <Link :to="{ name: 'home' }">← Retour au tableau de bord</Link>
    </template>
  </Card>
</template>
