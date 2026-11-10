<script setup lang="ts" generic="T extends string | number | null">
/**
 * Liste déroulante aux couleurs du site, à la place du <select> natif : le menu ouvert d'un
 * <select> est celui du système (impossible à styliser sous Safari).
 * Accessible : bouton role="combobox" relié à son <label for> par l'id, liste role="listbox",
 * clavier complet (voir use-select.ts). La liste est rendue dans <body> (voir use-dropdown.ts).
 * Slot « trigger » : contenu du bouton (par défaut, le libellé de l'option choisie).
 * Utilisé par : room-select, user-role-select, report-material-select, filtres et formulaires,
 * dev-user-switch (slot « trigger »).
 */
import {useSelect} from '../composables/use-select.ts';
import type {SelectOption} from '../composables/use-listbox.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** id du bouton, relié au <label for> du parent. */
  id: string;
  /** Options proposées, dans l'ordre d'affichage. */
  options: SelectOption<T>[];
  /** Texte affiché tant qu'aucune option ne correspond à la valeur (ex. « Choisir un bâtiment »). */
  placeholder?: string;
  disabled?: boolean;
}>();

/** Valeur de l'option choisie. */
const model = defineModel<T>({required: true});

//------------------ DATAS -----------------------------------
const {
  rows, selectedLabel, triggerClass, activeDescendant, optionClass, isSelected,
  isOpen, position, listId, optionId, setActive, choose, onClick, onKeydown,
} = useSelect<T>({
  id: props.id,
  options: () => props.options,
  placeholder: () => props.placeholder,
  disabled: () => props.disabled === true,
  model,
});
</script>

<template>
  <button
      :id="id"
      ref="trigger"
      type="button"
      class="select-trigger"
      :class="triggerClass"
      role="combobox"
      aria-haspopup="listbox"
      :aria-expanded="isOpen"
      :aria-controls="listId"
      :aria-activedescendant="activeDescendant"
      :disabled="disabled"
      @click="onClick"
      @keydown="onKeydown"
  >
    <slot name="trigger" :label="selectedLabel">
      <span class="select-value">{{ selectedLabel }}</span>
    </slot>
  </button>
  <Teleport to="body">
    <Transition name="select-pop">
      <ul
          v-if="isOpen"
          :id="listId"
          ref="panel"
          class="select-panel"
          role="listbox"
          :aria-labelledby="id"
          :style="position"
      >
        <template v-for="row in rows" :key="row.key">
          <li
              v-if="row.isGroup"
              class="select-group"
              role="presentation"
          >
            {{ row.label }}
          </li>
          <li
              v-else
              :id="optionId(row.index)"
              class="select-option"
              :class="optionClass(row.index)"
              role="option"
              :aria-selected="isSelected(row.index)"
              :aria-disabled="row.disabled"
              @pointermove="setActive(row.index)"
              @click="choose(row.index)"
          >
            {{ row.label }}
          </li>
        </template>
      </ul>
    </Transition>
  </Teleport>
</template>
