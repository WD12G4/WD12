<script setup lang="ts">
/**
 * Formulaire de création ou de modification d'un bâtiment (ADMIN).
 * Composant de présentation : il émet la saisie, la vue appelle le service.
 * Utilisé par : building-view.vue
 * RG : RG-12 (numéro entre 0 et 999, unique), RG-31 (réservé à l'administrateur)
 */
import {computed, ref, watch} from 'vue';
import {type Building, type BuildingInput, buildingNumberRange} from '../../../models/building.model.ts';
import FormError from '../../../shared/components/form-error.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Bâtiment à modifier, ou null pour une création. */
  initial: Building | null;
  /** Vrai pendant l'envoi. */
  submitting: boolean;
  /** Erreur renvoyée par l'API (numéro déjà pris…), ou null. */
  errorMessage: string | null;
}>();

const emit = defineEmits<{
  /** Saisie validée. */
  submit: [input: BuildingInput];
  /** Abandon de la modification. */
  cancel: [];
}>();

//------------------ DATAS -----------------------------------
const draft = ref<BuildingInput>({number: null});

//------------------ COMPUTED --------------------------------
/** RG-12 : un entier entre 0 et 999. */
const isValid = computed(() => {
  const value = draft.value.number;
  return value !== null && Number.isInteger(value)
      && value >= buildingNumberRange.min && value <= buildingNumberRange.max;
});
/** Le bouton d'envoi est actif si la saisie est valide et qu'aucun envoi n'est en cours. */
const canSubmit = computed(() => isValid.value && !props.submitting);
const isEditing = computed(() => props.initial !== null);
const submitLabel = computed(() => (isEditing.value ? 'Enregistrer' : 'Ajouter'));

//------------------ FUNCTIONS -------------------------------
/**
 * Émet la saisie. Le champ n'est pas vidé ici : en cas d'erreur de l'API, la saisie est gardée.
 * La vue réinitialise le formulaire après un succès (changement de :key).
 */
function submit(): void {
  if (!isValid.value) return;
  emit('submit', {...draft.value});
}

//------------------ LIFECYCLE -------------------------------
watch(() => props.initial, building => {
  draft.value = {number: building?.number ?? null};
}, {immediate: true});
</script>

<template>
  <form class="form-grid" @submit.prevent="submit">
    <div class="field">
      <label for="building-number">Numéro du bâtiment</label>
      <input
          id="building-number"
          v-model.number="draft.number"
          type="number"
          :min="buildingNumberRange.min"
          :max="buildingNumberRange.max"
          required
      >
    </div>
    <div class="full-width">
      <FormError :message="errorMessage" />
      <div class="actions">
        <button type="submit" :disabled="!canSubmit">{{ submitLabel }}</button>
        <button
            v-if="isEditing"
            type="button"
            class="secondary-button"
            @click="emit('cancel')"
        >
          Annuler
        </button>
      </div>
    </div>
  </form>
</template>
