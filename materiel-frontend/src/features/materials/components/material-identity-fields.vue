<script setup lang="ts">
/**
 * Champs d'identité du matériel : numéro de série et type.
 * Relié au brouillon du formulaire par v-model (defineModel).
 * Utilisé par : material-form.vue
 * RG : RG-01 (numéro de série obligatoire, 100 caractères maximum), RG-03 (type obligatoire)
 */
import {
  type MaterialInput, type MaterialType, materialTypeLabels, materialTypes, serialNumberMaxLength,
} from '../../../models/material.model.ts';
import type {SelectOption} from '../../../shared/composables/use-listbox.ts';
import AppSelect from '../../../shared/components/app-select.vue';

//------------------ PROPS / EMITS ---------------------------
/** Brouillon du formulaire, partagé avec material-form.vue. */
const draft = defineModel<MaterialInput>({required: true});

//------------------ DATAS -----------------------------------
/** RG-03 : le type est choisi parmi ceux de l'API. */
const typeOptions: SelectOption<MaterialType>[] = materialTypes.map(type => ({value: type, label: materialTypeLabels[type]}));
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
    <AppSelect
        id="material-type"
        v-model="draft.materialType"
        :options="typeOptions"
    />
  </div>
</template>
