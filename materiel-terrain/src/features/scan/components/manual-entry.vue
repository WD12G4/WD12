<script setup lang="ts">
/**
 * Saisie manuelle : complète le scan quand l'étiquette est abîmée ou absente.
 * Accepte un numéro de série ou le texte complet d'un code (analysé comme un scan).
 * Utilisé par : features/scan/views/scan-view.vue
 */
import {ref} from 'vue';

//------------------ PROPS / EMITS ---------------------------
const emit = defineEmits<{
  /** Texte saisi, non vide. */
  submit: [text: string];
}>();

//------------------ DATAS -----------------------------------
const text = ref('');

//------------------ FUNCTIONS -------------------------------
/**
 * Collage d'un texte sur plusieurs lignes (texte copié depuis l'appareil photo de l'iPhone) :
 * un champ texte supprimerait les retours à la ligne et collerait les mots entre eux.
 */
function onPaste(event: ClipboardEvent): void {
  const pasted = event.clipboardData?.getData('text') ?? '';
  if (!/[\r\n]/.test(pasted)) return;
  event.preventDefault();
  text.value = pasted.replace(/\s*[\r\n]+\s*/g, ' ').trim();
}

/** Envoie la saisie puis vide le champ pour la suivante. */
function submit(): void {
  if (text.value.trim().length === 0) return;
  emit('submit', text.value);
  text.value = '';
}
</script>

<template>
  <form class="manual-entry" @submit.prevent="submit">
    <label for="manual-serial" class="mono small">SAISIE MANUELLE</label>
    <div class="row">
      <input
          id="manual-serial"
          v-model="text"
          autocapitalize="characters"
          autocomplete="off"
          spellcheck="false"
          enterkeyhint="search"
          placeholder="N° de série ou texte du code"
          @paste="onPaste"
      >
      <button type="submit" class="secondary" :disabled="text.trim().length === 0">Identifier</button>
    </div>
  </form>
</template>
