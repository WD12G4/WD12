<script setup lang="ts">
/**
 * Liste déroulante du matériel d'une salle, pour désigner le matériel en panne.
 * Utilisé par : report-form.vue
 * RG : RG-21
 */
import {computed} from 'vue';
import {type Material, materialTypeLabels} from '../../../models/material.model.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** id de la balise <select>, relié au <label for> du parent. */
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

//------------------ FUNCTIONS -------------------------------
/** Libellé d'une option : « Écran – SN-0042 ». */
function optionLabel(material: Material): string {
  return `${materialTypeLabels[material.materialType]} – ${material.serialNumber}`;
}
</script>

<template>
  <select
      :id="id"
      v-model="materialId"
      :disabled="noRoom"
  >
    <option :value="null">Aucun matériel précis</option>
    <option
        v-for="material in roomMaterials"
        :key="material.id"
        :value="material.id"
    >
      {{ optionLabel(material) }}
    </option>
  </select>
</template>
