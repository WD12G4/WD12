<script setup lang="ts">
/**
 * Nombre qui défile jusqu'à sa valeur (à l'affichage, puis à chaque changement).
 * Sans animation si l'utilisateur a demandé moins de mouvement dans les réglages du système.
 * Utilisé par : features/home/components/stats-panel.vue
 */
import {onBeforeUnmount, ref, watch} from 'vue';
import {prefersReducedMotion} from '../composables/use-motion.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Valeur à atteindre. */
  value: number;
}>();

/** Durée du défilement, en millisecondes. */
const DURATION_MS = 700;

//------------------ DATAS -----------------------------------
const shown = ref(0);
let frame = 0;

//------------------ FUNCTIONS -------------------------------
/** Décélération douce : rapide au début, lente à l'arrivée. */
function easeOutCubic(progress: number): number {
  return 1 - (1 - progress) ** 3;
}

/**
 * Fait défiler le nombre affiché de sa valeur actuelle jusqu'à la cible.
 * @param target valeur à atteindre
 */
function animateTo(target: number): void {
  cancelAnimationFrame(frame);
  if (prefersReducedMotion()) {
    shown.value = target;
    return;
  }
  const from = shown.value;
  const start = performance.now();
  const step = (now: number) => {
    const progress = Math.min((now - start) / DURATION_MS, 1);
    shown.value = Math.round(from + (target - from) * easeOutCubic(progress));
    if (progress < 1) frame = requestAnimationFrame(step);
  };
  frame = requestAnimationFrame(step);
}

//------------------ LIFECYCLE -------------------------------
watch(() => props.value, animateTo, {immediate: true});
onBeforeUnmount(() => cancelAnimationFrame(frame));
</script>

<template>
  <span>{{ shown }}</span>
</template>
