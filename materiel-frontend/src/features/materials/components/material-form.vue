<script setup lang="ts">
/**
 * Formulaire de création ou de modification d'un matériel (gestionnaire et administrateur).
 * Composant de présentation : il émet la saisie, la vue appelle le service et lui repasse l'erreur.
 * Utilisé par : material-create-view.vue, material-edit-view.vue
 * RG : RG-01, RG-03, RG-04, RG-05, RG-06, RG-16
 */
import {computed, ref, watch} from 'vue';
import type {Material, MaterialInput} from '../../../models/material.model.ts';
import type {Room} from '../../../models/room.model.ts';
import {toIsoDate} from '../../../shared/composables/use-dates.ts';
import FormError from '../../../shared/components/form-error.vue';
import MaterialIdentityFields from './material-identity-fields.vue';
import MaterialStateFields from './material-state-fields.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Matériel à modifier, ou null pour une création. */
  initial: Material | null;
  /** Salles proposées. */
  rooms: Room[];
  /** Vrai pendant l'envoi : évite un double envoi. */
  submitting: boolean;
  /** Erreur renvoyée par l'API, ou null. */
  errorMessage: string | null;
}>();

const emit = defineEmits<{
  /** Saisie validée, à enregistrer. */
  submit: [input: MaterialInput];
  /** L'utilisateur abandonne la saisie. */
  cancel: [];
}>();

//------------------ DATAS -----------------------------------
const defaultMaterial: Readonly<MaterialInput> = {
  serialNumber: '',
  materialType: 'UC',
  endAvailabilityDate: null,
  roomId: null,
  archived: false,
};

const draft = ref<MaterialInput>({...defaultMaterial});

//------------------ COMPUTED --------------------------------
/** Champs obligatoires remplis (RG-01, RG-03, RG-04 tant que l'API exige la date). */
const isValid = computed(() =>
  draft.value.serialNumber.trim().length > 0 && draft.value.endAvailabilityDate !== null,
);
/** Le bouton d'envoi est actif si la saisie est valide et qu'aucun envoi n'est en cours. */
const canSubmit = computed(() => isValid.value && !props.submitting);
const submitLabel = computed(() => (props.initial ? 'Enregistrer' : 'Créer le matériel'));

//------------------ FUNCTIONS -------------------------------
/** Convertit un matériel existant en brouillon de formulaire. */
function toInput(material: Material): MaterialInput {
  return {
    serialNumber: material.serialNumber,
    materialType: material.materialType,
    endAvailabilityDate: material.endAvailabilityDate ? toIsoDate(material.endAvailabilityDate) : null,
    roomId: material.room?.id ?? null,
    archived: material.archived,
  };
}

/** Émet la saisie si elle est valide. Le brouillon est gardé : la vue change de page en cas de succès. */
function submit(): void {
  if (!isValid.value) return;
  emit('submit', {...draft.value});
}

//------------------ LIFECYCLE -------------------------------
// En modification, le brouillon est rempli avec le matériel reçu (et rechargé s'il change).
watch(() => props.initial, material => {
  draft.value = material ? toInput(material) : {...defaultMaterial};
}, {immediate: true});
</script>

<template>
  <!-- « stepped » : chaque champ est numéroté (01, 02…) par un compteur CSS -->
  <form class="form-grid stepped" @submit.prevent="submit">
    <MaterialIdentityFields v-model="draft" />
    <MaterialStateFields v-model="draft" :rooms="rooms" />
    <div class="full-width">
      <FormError :message="errorMessage" />
      <div class="actions">
        <button type="submit" :disabled="!canSubmit">{{ submitLabel }}</button>
        <button
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
