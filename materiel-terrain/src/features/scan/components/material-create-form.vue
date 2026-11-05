<script setup lang="ts">
/**
 * Prise en compte d'un matériel non recensé, pré-remplie avec la lecture (numéro, type suggéré par le NNO).
 * Le matériel est placé dans la salle où il a été trouvé. Composant de présentation : la vue envoie.
 * Utilisé par : scan-result-sheet.vue
 * RG : RG-01 (numéro unique, 100 caractères), RG-05 (date de fin non passée)
 */
import {computed, ref} from 'vue';
import {
  type MaterialInput, type MaterialType, materialTypeLabels, materialTypes, serialNumberMaxLength,
} from '../../../models/material.model.ts';
import type {Room} from '../../../models/room.model.ts';
import type {ScanReading} from '../../../models/scan.model.ts';
import {todayIsoDate} from '../../../shared/composables/use-dates.ts';
import {locationLabel} from '../../../shared/composables/use-room-label.ts';
import FormError from '../../../shared/components/form-error.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  reading: ScanReading;
  /** Salle courante de l'agent, ou null (le matériel sera en stock). */
  room: Room | null;
  submitting: boolean;
  errorMessage: string | null;
}>();

const emit = defineEmits<{
  submit: [input: MaterialInput];
}>();

//------------------ DATAS -----------------------------------
const serialNumber = ref(props.reading.serialNumber);
const materialType = ref<MaterialType | ''>(props.reading.typeHint ?? '');
const endAvailabilityDate = ref('');
const minDate = todayIsoDate();

//------------------ COMPUTED --------------------------------
const canSubmit = computed(() => !props.submitting && serialNumber.value.trim().length > 0
    && materialType.value !== '' && endAvailabilityDate.value >= minDate);

//------------------ FUNCTIONS -------------------------------
function submit(): void {
  if (!canSubmit.value || materialType.value === '') return;
  emit('submit', {
    serialNumber: serialNumber.value,
    materialType: materialType.value,
    endAvailabilityDate: endAvailabilityDate.value,
    roomId: props.room?.id ?? null,
  });
}
</script>

<template>
  <form class="form-grid" @submit.prevent="submit">
    <div class="field">
      <label for="create-serial">Numéro de série</label>
      <input id="create-serial" v-model="serialNumber" class="mono" :maxlength="serialNumberMaxLength"
             autocapitalize="characters" autocomplete="off" spellcheck="false" required>
    </div>
    <div class="field">
      <label for="create-type">Type</label>
      <select id="create-type" v-model="materialType" required>
        <option value="" disabled>Choisir…</option>
        <option v-for="type in materialTypes" :key="type" :value="type">{{ materialTypeLabels[type] }}</option>
      </select>
      <small v-if="reading.typeHint" class="muted">Suggéré d'après le n° OTAN, à vérifier.</small>
    </div>
    <div class="field">
      <label for="create-end">Fin de disponibilité</label>
      <input id="create-end" v-model="endAvailabilityDate" type="date" :min="minDate" required>
    </div>
    <p class="muted small">Emplacement : <strong>{{ locationLabel(room) }}</strong></p>
    <FormError :message="errorMessage" />
    <button type="submit" class="primary" :disabled="!canSubmit">
      {{ submitting ? 'Enregistrement…' : 'Prendre en compte' }}
    </button>
  </form>
</template>
