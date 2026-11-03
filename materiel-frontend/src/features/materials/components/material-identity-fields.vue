<script setup lang="ts">
/**
 * Champs d'identité du matériel : numéro de série et type.
 * Relié au brouillon du formulaire par v-model (defineModel).
 * Utilisé par : material-form.vue
 * RG : RG-01 (numéro de série obligatoire, 100 caractères maximum), RG-03 (type obligatoire)
 */
import {type MaterialInput, materialTypeLabels, materialTypes, serialNumberMaxLength} from '../../../models/material.model.ts';

//------------------ PROPS / EMITS ---------------------------
/** Brouillon du formulaire, partagé avec material-form.vue. */
const draft = defineModel<MaterialInput>({required: true});
</script>

<template>
  <div class="field">
    <label for="material-serial">Numéro de série</label>
    <input
        id="material-serial"
        v-model="draft.serialNumber"
        :maxlength="serialNumberMaxLength"
        placeholder="Ex. SN-2026-0042"
        required
    >
  </div>
  <div class="field">
    <label for="material-type">Type</label>
    <select
        id="material-type"
        v-model="draft.materialType"
        required
    >
      <option
          v-for="type in materialTypes"
          :key="type"
          :value="type"
      >
        {{ materialTypeLabels[type] }}
      </option>
    </select>
  </div>
</template>
