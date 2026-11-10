<script setup lang="ts">
/**
 * Panneau latéral en verre : s'ouvre à droite sans quitter la page. Sur grand écran, la page reste
 * cliquable (un clic sur un autre élément change le contenu du panneau) ; sur mobile, un voile la
 * couvre. Échap ou × ferme ; le focus revient à l'élément qui a ouvert le panneau.
 * Le contenu vient du slot par défaut, les boutons du slot « actions ».
 * Utilisé par : material-preview.vue, workstation-panel.vue
 */
import {computed, nextTick, onBeforeUnmount, useId, useTemplateRef, watch} from 'vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Identifiant de l'élément affiché, ou null quand le panneau est fermé. */
  panelKey: string | number | null;
  /** Titre du panneau. */
  title: string;
  /** Petit libellé au-dessus du titre (ex. « Aperçu du matériel »). */
  eyebrow: string;
  /** Classe du titre (ex. « code » pour un numéro de série). */
  titleClass?: string;
}>();

const emit = defineEmits<{
  /** Fermeture demandée (×, Échap, voile). */
  close: [];
}>();

//------------------ DATAS -----------------------------------
const titleId = useId();
const closeButton = useTemplateRef<HTMLButtonElement>('closeButton');
/** Élément qui avait le focus à l'ouverture, pour le lui rendre à la fermeture. */
let opener: HTMLElement | null = null;

//------------------ COMPUTED --------------------------------
const isOpen = computed(() => props.panelKey !== null);

//------------------ FUNCTIONS -------------------------------
/** Échap ferme le panneau, même si le focus est resté dans la page (grand écran). */
function onKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') emit('close');
}

//------------------ LIFECYCLE -------------------------------
// À l'ouverture : focus sur × et écoute d'Échap. Changement d'élément, panneau ouvert : on retient
// le nouvel élément cliqué, sans lui prendre le focus. À la fermeture : focus rendu au dernier élément cliqué.
watch(() => props.panelKey, (key, previousKey) => {
  if (key === null) {
    document.removeEventListener('keydown', onKeydown);
    opener?.focus();
    return;
  }
  opener = document.activeElement as HTMLElement | null;
  if (previousKey !== null && previousKey !== undefined) return;
  document.addEventListener('keydown', onKeydown);
  void nextTick(() => closeButton.value?.focus());
});
onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown));
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
          v-if="isOpen"
          class="drawer-backdrop"
          @click="emit('close')"
      />
    </Transition>
    <Transition name="drawer">
      <aside
          v-if="isOpen"
          class="drawer"
          role="dialog"
          :aria-labelledby="titleId"
      >
        <header class="drawer-header">
          <div>
            <span class="eyebrow">{{ eyebrow }}</span>
            <h2 :id="titleId" :class="titleClass">{{ title }}</h2>
          </div>
          <button
              ref="closeButton"
              type="button"
              class="icon-button"
              aria-label="Fermer le panneau"
              @click="emit('close')"
          >
            ×
          </button>
        </header>
        <slot />
        <div v-if="$slots.actions" class="actions">
          <slot name="actions" />
        </div>
      </aside>
    </Transition>
  </Teleport>
</template>
