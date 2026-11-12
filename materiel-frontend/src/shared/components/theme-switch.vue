<script setup lang="ts">
/**
 * Apparence : bouton palette du menu, qui ouvre un panneau en verre avec le choix du thème (Givre,
 * Nuit, Aurore, Lagon, Sauge) et de la disposition du menu (layout-options.vue). Chaque thème a un aperçu vivant : une vignette qui porte son data-theme, donc
 * ses vraies couleurs (fond à reflets, plaque de verre, pastille d'accent). Échap ou un clic
 * ailleurs referment le panneau ; il reste ouvert après un choix, pour comparer. Le panneau est rendu dans <body> : dans la barre de menu (déjà
 * floutée), son verre ne flouterait pas la page dessous.
 * Utilisé par : app-nav.vue
 */
import {computed, type CSSProperties, nextTick, onBeforeUnmount, ref, useTemplateRef, watch} from 'vue';
import {useTheme} from '../composables/use-theme.ts';
import AppIcon from './app-icon.vue';
import LayoutOptions from './layout-options.vue';

//------------------ DATAS -----------------------------------
const {themes, currentTheme, currentLayout, setTheme} = useTheme();
/** Sous cette largeur, le menu est un tiroir : le panneau s'ouvre en bas de l'écran. */
const DRAWER_MAX_WIDTH_PX = 900;
const open = ref(false);
const root = useTemplateRef<HTMLElement>('root');
const panel = useTemplateRef<HTMLElement>('panel');
const panelStyle = ref<CSSProperties>({});

//------------------ COMPUTED --------------------------------
const buttonLabel = computed(() => `Thème : ${themes.find(theme => theme.id === currentTheme.value)?.label ?? ''}`);

//------------------ FUNCTIONS -------------------------------
/**
 * Place le panneau : sous le bouton (barre du haut), à sa droite (barre latérale), ou en bas de
 * l'écran dans le tiroir (mobile).
 */
function place(): void {
  const rect = root.value?.getBoundingClientRect();
  if (!rect) return;
  if (window.innerWidth <= DRAWER_MAX_WIDTH_PX) panelStyle.value = {left: '1rem', right: '1rem', bottom: '1rem'};
  else if (currentLayout.value === 'sidebar') {
    const menuRight = root.value?.closest('.main-menu')?.getBoundingClientRect().right ?? rect.right;
    panelStyle.value = {left: `${menuRight + 12}px`, bottom: `${Math.max(8, window.innerHeight - rect.bottom)}px`};
  }
  else panelStyle.value = {top: `${rect.bottom + 12}px`, right: `${Math.max(8, window.innerWidth - rect.right - 48)}px`};
}

/** Ferme le panneau sur Échap, ou sur un clic en dehors du bouton et du panneau. */
function onDocumentEvent(event: Event): void {
  const inside = event.target instanceof Node && (root.value?.contains(event.target) || panel.value?.contains(event.target));
  const outside = event instanceof PointerEvent && !inside;
  if (outside || (event instanceof KeyboardEvent && event.key === 'Escape')) open.value = false;
}


/** Écoute (ou n'écoute plus) les clics et Échap dans la page, tant que le panneau est ouvert. */
function listen(enabled: boolean): void {
  const action = enabled ? document.addEventListener : document.removeEventListener;
  action.call(document, 'pointerdown', onDocumentEvent);
  action.call(document, 'keydown', onDocumentEvent);
}

//------------------ LIFECYCLE -------------------------------
watch(currentLayout, () => void nextTick(place));
watch(open, isOpen => {
  if (isOpen) place();
  listen(isOpen);
});
onBeforeUnmount(() => listen(false));
</script>

<template>
  <div ref="root" class="theme-switch">
    <button
        type="button"
        class="icon-button"
        :aria-label="buttonLabel"
        :title="buttonLabel"
        :aria-expanded="open"
        @click="open = !open"
    >
      <AppIcon name="palette" />
    </button>
    <Teleport to="body">
      <Transition name="select-pop">
        <div
            v-if="open"
            ref="panel"
            class="theme-panel"
            :style="panelStyle"
        >
          <span class="eyebrow">Thème</span>
          <div
              class="theme-options"
              role="radiogroup"
              aria-label="Thème"
          >
            <button
                v-for="theme in themes"
                :key="theme.id"
                type="button"
                role="radio"
                class="theme-option"
                :aria-checked="theme.id === currentTheme"
                @click="setTheme(theme.id)"
            >
              <span class="theme-preview" :data-theme="theme.id">
                <span class="theme-preview-glass"><span class="theme-preview-dot" /></span>
              </span>
              <span class="theme-option-text">
                <strong>{{ theme.label }}</strong>
                <span>{{ theme.description }}</span>
              </span>
            </button>
          </div>
          <span class="eyebrow">Disposition</span>
          <LayoutOptions />
        </div>
      </Transition>
    </Teleport>
  </div>
</template>
