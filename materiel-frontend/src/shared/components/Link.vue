<script setup lang="ts">
/**
 * Lien vers une route nommée, avec une cible typée (RouteTarget).
 * Copié du projet du cours (vue1) : nom de fichier gardé tel quel.
 * Utilisé par : toutes les vues et composants qui affichent un lien interne.
 */
import {computed, type PropType} from 'vue';
import {RouterLink, type RouteLocationRaw} from 'vue-router';
import type {RouteTarget} from '../../router/route-types.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps({
  /** Route cible : { name } ou { name, params }. Un nom inconnu est refusé à la compilation. */
  to: {
    type: Object as PropType<RouteTarget>,
    required: true,
  },
});

//------------------ COMPUTED --------------------------------
/** RouterLink attend un RouteLocationRaw : RouteTarget en est un sous-type. */
const routerTo = computed(() => props.to as RouteLocationRaw);
</script>

<template>
  <RouterLink :to="routerTo">
    <slot />
  </RouterLink>
</template>
