<script setup lang="ts" generic="T extends string | number">
/**
 * Boutons segmentés : toutes les options visibles, un seul clic pour choisir (au lieu d'ouvrir
 * puis de choisir dans une liste déroulante). Pour quelques options courtes seulement.
 * Accessible comme un groupe de boutons radio : role="radiogroup", un seul arrêt de tabulation,
 * flèches gauche/droite (ou haut/bas), Début et Fin pour changer d'option.
 * Utilisé par : filtres du matériel, horizon des fins de disponibilité, choix du thème.
 */
import {nextTick, useTemplateRef} from 'vue';
import type {SelectOption} from '../composables/use-listbox.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Options, dans l'ordre d'affichage. */
  options: SelectOption<T>[];
  /** id de l'élément qui nomme le groupe (intitulé visible au-dessus). */
  labelledby: string;
}>();

/** Valeur de l'option choisie. */
const model = defineModel<T>({required: true});

//------------------ DATAS -----------------------------------
const group = useTemplateRef<HTMLElement>('group');

/** Déplacement de chaque touche (Début et Fin sont gérés à part). */
const steps: Record<string, number> = {ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1};

//------------------ FUNCTIONS -------------------------------
const isSelected = (option: SelectOption<T>): boolean => option.value === model.value;
/** Seule l'option choisie reçoit le focus par Tab (comme un groupe radio natif). */
const tabIndex = (option: SelectOption<T>): number => (isSelected(option) ? 0 : -1);

/** Choisit l'option d'index donné et lui donne le focus. */
function select(index: number): void {
  const option = props.options[index];
  if (!option) return;
  model.value = option.value;
  void nextTick(() => group.value?.querySelectorAll('button')[index]?.focus());
}

/** Clavier : flèches pour l'option voisine (en boucle), Début et Fin pour les extrémités. */
function onKeydown(event: KeyboardEvent): void {
  const count = props.options.length;
  const current = props.options.findIndex(isSelected);
  let next = -1;
  if (event.key === 'Home') next = 0;
  else if (event.key === 'End') next = count - 1;
  else if (event.key in steps) next = (current + (steps[event.key] ?? 0) + count) % count;
  if (next < 0) return;
  event.preventDefault();
  select(next);
}
</script>

<template>
  <div
      ref="group"
      class="segmented"
      role="radiogroup"
      :aria-labelledby="labelledby"
      @keydown="onKeydown"
  >
    <button
        v-for="(option, index) in options"
        :key="String(option.value)"
        type="button"
        role="radio"
        :aria-checked="isSelected(option)"
        :tabindex="tabIndex(option)"
        @click="select(index)"
    >
      {{ option.label }}
    </button>
  </div>
</template>
