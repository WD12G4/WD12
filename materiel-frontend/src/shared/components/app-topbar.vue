<script setup lang="ts">
/**
 * Barre du haut, affichée seulement sur mobile : marque et bouton d'ouverture du menu.
 * Composant de présentation : l'état du menu est tenu par App.vue.
 * Utilisé par : App.vue
 */
import AppIcon from './app-icon.vue';
import BrandMark from './brand-mark.vue';
import BrandWordmark from './brand-wordmark.vue';
import Link from './Link.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Vrai si le tiroir du menu est ouvert. */
  menuOpen: boolean;
}>();

const emit = defineEmits<{
  /** L'utilisateur ouvre ou ferme le menu. */
  toggle: [];
}>();
</script>

<template>
  <header class="topbar">
    <button
        type="button"
        class="icon-button"
        aria-label="Menu principal"
        aria-controls="main-menu"
        :aria-expanded="menuOpen"
        @click="emit('toggle')"
    >
      <AppIcon name="menu" />
    </button>
    <!-- Logo et nom : lien vers le tableau de bord -->
    <Link
        class="brand"
        :to="{ name: 'home' }"
        aria-label="BASTION, tableau de bord"
    >
      <BrandMark class="brand-logo" />
      <BrandWordmark class="brand-name" />
    </Link>
  </header>
</template>
