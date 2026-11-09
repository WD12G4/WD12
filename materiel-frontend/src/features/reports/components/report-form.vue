<script setup lang="ts">
/**
 * Formulaire de remontée : salle, matériel et type de problème en listes déroulantes,
 * description courte facultative (RG-21 : « pas besoin d'un gros formulaire »).
 * Composant de présentation : il émet la saisie, la vue appelle le service.
 * Utilisé par : report-create-view.vue
 */
import {computed, ref, watch} from 'vue';
import type {Material} from '../../../models/material.model.ts';
import type {Room} from '../../../models/room.model.ts';
import {
  type ProblemType, problemTypeLabels, problemTypes, reportDescriptionMaxLength, type ReportInput,
} from '../../../models/report.model.ts';
import type {SelectOption} from '../../../shared/composables/use-listbox.ts';
import AppSelect from '../../../shared/components/app-select.vue';
import FormError from '../../../shared/components/form-error.vue';
import RoomSelect from '../../../shared/components/room-select.vue';
import ReportMaterialSelect from './report-material-select.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Salles proposées. */
  rooms: Room[];
  /** Matériel proposé (filtré par salle dans la liste). */
  materials: Material[];
  /** Vrai pendant l'envoi. */
  submitting: boolean;
  /** Erreur renvoyée, ou null. */
  errorMessage: string | null;
}>();

const emit = defineEmits<{
  /** Remontée à enregistrer. */
  submit: [input: ReportInput];
}>();

//------------------ DATAS -----------------------------------
const draft = ref<ReportInput>({
  roomId: null,
  materialId: null,
  problemType: 'OUT_OF_ORDER',
  description: '',
});
/** RG-21 : types de problème proposés (liste à valider avec le client). */
const problemTypeOptions: SelectOption<ProblemType>[] = problemTypes.map(type => ({value: type, label: problemTypeLabels[type]}));

//------------------ COMPUTED --------------------------------
const isValid = computed(() => draft.value.roomId !== null);
/** Le bouton d'envoi est actif si la saisie est valide et qu'aucun envoi n'est en cours. */
const canSubmit = computed(() => isValid.value && !props.submitting);

//------------------ FUNCTIONS -------------------------------
/** Émet la saisie si une salle est choisie (seul champ obligatoire). */
function submit(): void {
  if (!isValid.value) return;
  emit('submit', {...draft.value});
}

//------------------ LIFECYCLE -------------------------------
// Le matériel choisi n'a plus de sens si l'on change de salle.
watch(() => draft.value.roomId, () => {
  draft.value.materialId = null;
});
</script>

<template>
  <form class="form-grid" @submit.prevent="submit">
    <div class="field">
      <label for="report-room">Salle</label>
      <RoomSelect
          id="report-room"
          v-model="draft.roomId"
          :rooms="rooms"
          empty-label="Choisir une salle"
      />
    </div>
    <div class="field">
      <label for="report-material">Matériel concerné</label>
      <ReportMaterialSelect
          id="report-material"
          v-model="draft.materialId"
          :materials="materials"
          :room-id="draft.roomId"
      />
    </div>
    <div class="field">
      <label for="report-type">Type de problème</label>
      <AppSelect
          id="report-type"
          v-model="draft.problemType"
          :options="problemTypeOptions"
      />
    </div>
    <div class="field full-width">
      <label for="report-description">Description <span class="field-hint">(facultative)</span></label>
      <textarea
          id="report-description"
          v-model="draft.description"
          :maxlength="reportDescriptionMaxLength"
          rows="3"
          placeholder="Ex. l'écran ne s'allume plus"
      />
    </div>
    <div class="full-width">
      <FormError :message="errorMessage" />
      <button type="submit" :disabled="!canSubmit">Envoyer la remontée</button>
    </div>
  </form>
</template>
