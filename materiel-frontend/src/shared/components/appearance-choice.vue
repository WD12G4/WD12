<script setup lang="ts" generic="T extends string">
/**
 * Choix d'apparence en deux options ou plus, chacune avec un petit schéma (classe .layout-preview
 * et l'identifiant de l'option : top, sidebar, card, open). Sert à la disposition du menu et à
 * l'affichage du contenu.
 * Composant de présentation.
 * Utilisé par : theme-switch.vue (panneau d'apparence)
 */

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Options proposées. */
  options: readonly { id: T; label: string }[];
  /** Option choisie. */
  selected: T;
  /** Nom accessible du groupe. */
  label: string;
}>();

const emit = defineEmits<{
  /** Une option est choisie. */
  choose: [id: T];
}>();
</script>

<template>
  <div
      class="layout-options"
      role="radiogroup"
      :aria-label="label"
  >
    <button
        v-for="option in options"
        :key="option.id"
        type="button"
        role="radio"
        class="layout-option"
        :aria-checked="option.id === selected"
        @click="emit('choose', option.id)"
    >
      <span
          class="layout-preview"
          :class="option.id"
          aria-hidden="true"
      >
        <span />
      </span>
      {{ option.label }}
    </button>
  </div>
</template>
