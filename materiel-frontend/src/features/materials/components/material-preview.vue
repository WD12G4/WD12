<script setup lang="ts">
/**
 * Aperçu latéral d'un matériel : la fiche s'ouvre à droite de la liste, sans la quitter ni perdre ses
 * filtres. Sur grand écran, la liste reste cliquable : un clic sur un autre matériel change l'aperçu.
 * Sur mobile, un voile couvre la liste. Échap ou × ferme ; le focus revient à l'élément d'origine.
 * Utilisé par : material-list.vue (liste du matériel, fiche d'une salle)
 * RG : RG-16 (« Modifier » réservé à l'administrateur)
 */
import {computed, nextTick, onBeforeUnmount, useTemplateRef, watch} from 'vue';
import type {Material} from '../../../models/material.model.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import Link from '../../../shared/components/Link.vue';
import MaterialDetails from './material-details.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Matériel affiché, ou null quand l'aperçu est fermé. */
  material: Material | null;
}>();

const emit = defineEmits<{
  /** Fermeture demandée (×, Échap, voile). */
  close: [];
}>();

//------------------ DATAS -----------------------------------
const {hasRole} = useAuth();
const closeButton = useTemplateRef<HTMLButtonElement>('closeButton');
/** Élément qui avait le focus à l'ouverture, pour le lui rendre à la fermeture. */
let opener: HTMLElement | null = null;

//------------------ COMPUTED --------------------------------
const canEdit = computed(() => hasRole('ADMIN'));
const materialId = computed(() => props.material?.id ?? 0);

//------------------ FUNCTIONS -------------------------------
/** Échap ferme l'aperçu, même si le focus est resté dans la liste (grand écran). */
function onKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') emit('close');
}

//------------------ LIFECYCLE -------------------------------
// À l'ouverture : focus sur × et écoute d'Échap. Changement de matériel, aperçu ouvert : on retient
// le nouveau lien cliqué, sans lui prendre le focus. À la fermeture : focus rendu au dernier lien cliqué.
watch(() => props.material?.id, (id, previousId) => {
  if (id === undefined) {
    document.removeEventListener('keydown', onKeydown);
    opener?.focus();
    return;
  }
  opener = document.activeElement as HTMLElement | null;
  if (previousId !== undefined) return;
  document.addEventListener('keydown', onKeydown);
  void nextTick(() => closeButton.value?.focus());
});
onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown));
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
          v-if="material"
          class="drawer-backdrop"
          @click="emit('close')"
      />
    </Transition>
    <Transition name="drawer">
      <aside
          v-if="material"
          class="drawer"
          role="dialog"
          aria-labelledby="material-preview-title"
      >
        <header class="drawer-header">
          <div>
            <span class="eyebrow">Aperçu du matériel</span>
            <h2 id="material-preview-title" class="code">{{ material.serialNumber }}</h2>
          </div>
          <button
              ref="closeButton"
              type="button"
              class="icon-button"
              aria-label="Fermer l'aperçu"
              @click="emit('close')"
          >
            ×
          </button>
        </header>
        <MaterialDetails :material="material" />
        <div class="actions">
          <Link class="button-link" :to="{ name: 'material-detail', params: { id: materialId } }">Ouvrir la fiche</Link>
          <Link
              v-if="canEdit"
              class="button-link secondary-button"
              :to="{ name: 'material-edit', params: { id: materialId } }"
          >
            Modifier
          </Link>
        </div>
      </aside>
    </Transition>
  </Teleport>
</template>
