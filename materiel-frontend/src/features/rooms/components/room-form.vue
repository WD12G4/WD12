<script setup lang="ts">
/**
 * Formulaire de création ou de modification d'une salle (ADMIN).
 * Composant de présentation : il émet la saisie, la vue appelle le service.
 * Utilisé par : room-view.vue
 * RG : RG-13 (un bâtiment, numéro entre 0 et 999 unique dans le bâtiment), RG-31
 */
import {computed, ref, watch} from 'vue';
import type {Building} from '../../../models/building.model.ts';
import {type Room, type RoomInput, roomNumberRange} from '../../../models/room.model.ts';
import type {SelectOption} from '../../../shared/composables/use-listbox.ts';
import AppSelect from '../../../shared/components/app-select.vue';
import FormError from '../../../shared/components/form-error.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Salle à modifier, ou null pour une création. */
  initial: Room | null;
  /** Bâtiments proposés. */
  buildings: Building[];
  /** Vrai pendant l'envoi. */
  submitting: boolean;
  /** Erreur renvoyée par l'API, ou null. */
  errorMessage: string | null;
}>();

const emit = defineEmits<{
  /** Saisie validée. */
  submit: [input: RoomInput];
  /** Abandon de la modification. */
  cancel: [];
}>();

//------------------ DATAS -----------------------------------
const draft = ref<RoomInput>({buildingId: null, number: null});

//------------------ COMPUTED --------------------------------
const buildingOptions = computed<SelectOption<number | null>[]>(() =>
  props.buildings.map(building => ({value: building.id, label: `Bâtiment ${building.number}`})));

/** RG-13 : un bâtiment choisi et un numéro entier entre 0 et 999. */
const isValid = computed(() => {
  const value = draft.value.number;
  return draft.value.buildingId !== null && value !== null && Number.isInteger(value)
      && value >= roomNumberRange.min && value <= roomNumberRange.max;
});
/** Le bouton d'envoi est actif si la saisie est valide et qu'aucun envoi n'est en cours. */
const canSubmit = computed(() => isValid.value && !props.submitting);
const isEditing = computed(() => props.initial !== null);
const submitLabel = computed(() => (isEditing.value ? 'Enregistrer' : 'Ajouter'));

//------------------ FUNCTIONS -------------------------------
/**
 * Émet la saisie. Le formulaire n'est pas vidé ici : en cas d'erreur de l'API, la saisie est gardée.
 * La vue le réinitialise après un succès (changement de :key).
 */
function submit(): void {
  if (!isValid.value) return;
  emit('submit', {...draft.value});
}

//------------------ LIFECYCLE -------------------------------
watch(() => props.initial, room => {
  draft.value = {buildingId: room?.building.id ?? null, number: room?.number ?? null};
}, {immediate: true});
</script>

<template>
  <form class="form-grid" @submit.prevent="submit">
    <div class="field">
      <label for="room-building">Bâtiment</label>
      <AppSelect
          id="room-building"
          v-model="draft.buildingId"
          :options="buildingOptions"
          placeholder="Choisir un bâtiment"
      />
    </div>
    <div class="field">
      <label for="room-number">Numéro de la salle</label>
      <input
          id="room-number"
          v-model.number="draft.number"
          type="number"
          :min="roomNumberRange.min"
          :max="roomNumberRange.max"
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
