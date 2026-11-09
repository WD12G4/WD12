<script setup lang="ts">
/**
 * Liste déroulante du matériel d'une salle, pour désigner le matériel en panne.
 * Utilisé par : report-form.vue
 * RG : RG-21
 */
import {computed} from 'vue';
import {type Material, materialTypeLabels} from '../../../models/material.model.ts';
import type {SelectOption} from '../../../shared/composables/use-listbox.ts';
import AppSelect from '../../../shared/components/app-select.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** id de la liste, relié au <label for> du parent. */
  id: string;
  /** Tout le matériel ; seul celui de la salle choisie est proposé. */
  materials: Material[];
  /** Salle choisie, ou null (la liste est alors désactivée). */
  roomId: number | null;
}>();

/** Identifiant du matériel choisi, ou null si le problème concerne la salle en général. */
const materialId = defineModel<number | null>({required: true});

//------------------ COMPUTED --------------------------------
/** Sans salle choisie, il n'y a pas de matériel à proposer. */
const noRoom = computed(() => props.roomId === null);
const roomMaterials = computed(() => props.materials.filter(material => material.room?.id === props.roomId));

/** Option « aucun matériel précis », puis le matériel de la salle. */
const options = computed<SelectOption<number | null>[]>(() => [
  {value: null, label: 'Aucun matériel précis'},
  ...roomMaterials.value.map(material => ({value: material.id, label: optionLabel(material)})),
]);

//------------------ FUNCTIONS -------------------------------
/** Libellé d'une option : « Écran – SN-0042 ». */
function optionLabel(material: Material): string {
  return `${materialTypeLabels[material.materialType]} – ${material.serialNumber}`;
}
</script>

<template>
  <AppSelect
      :id="id"
      v-model="materialId"
      :options="options"
      :disabled="noRoom"
  />
</template>
