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

/** Identifiant du matériel choisi, ou null tant qu'aucun n'est choisi. */
const materialId = defineModel<number | null>({required: true});

//------------------ COMPUTED --------------------------------
/** Sans salle choisie, il n'y a pas de matériel à proposer. */
const noRoom = computed(() => props.roomId === null);
const roomMaterials = computed(() => props.materials.filter(material => material.room?.id === props.roomId));

/** Le matériel de la salle (le backend rattache chaque remontée à un matériel). */
const options = computed<SelectOption<number | null>[]>(() => roomMaterials.value.map(material => ({
  value: material.id, label: optionLabel(material),
})));
/** Texte affiché tant qu'aucun matériel n'est choisi. */
const placeholder = computed(() => (noRoom.value ? 'Choisissez d\'abord une salle' : 'Choisir le matériel'));

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
      :placeholder="placeholder"
      :disabled="noRoom"
  />
</template>
