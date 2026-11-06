<script setup lang="ts">
/**
 * Navigation principale en bas d'écran, à portée de pouce : scanner, situation des salles, synchronisation.
 * Utilisé par : App.vue (agent connecté)
 */
import {computed} from 'vue';
import {useRoute} from 'vue-router';

//------------------ VALEURS ---------------------------------
const tabs = [
  {name: 'scan', icon: '⌖', label: 'Scanner'},
  {name: 'rooms', icon: '▦', label: 'Salles'},
  {name: 'sync', icon: '⇅', label: 'Synchro'},
] as const;

//------------------ DATAS -----------------------------------
const route = useRoute();

//------------------ COMPUTED --------------------------------
/** Onglet actif : le compte rendu d'une salle et le choix de position restent rattachés à leur onglet. */
const active = computed(() => {
  if (route.name === 'room') return 'rooms';
  return route.name === 'position' ? 'scan' : route.name;
});
</script>

<template>
  <nav class="tab-bar" aria-label="Navigation principale">
    <RouterLink v-for="tab in tabs" :key="tab.name" :to="{name: tab.name}" class="tab"
                :class="{active: active === tab.name}" :aria-current="active === tab.name ? 'page' : undefined">
      <span class="tab-icon" aria-hidden="true">{{ tab.icon }}</span>
      <span>{{ tab.label }}</span>
    </RouterLink>
  </nav>
</template>
