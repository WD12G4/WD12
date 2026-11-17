<script setup lang="ts">
/**
 * Champs d'identité du matériel : numéro de série et type.
 * Relié au brouillon du formulaire par v-model (defineModel).
 * Utilisé par : material-form.vue
 * RG : RG-01 (numéro de série obligatoire, 100 caractères maximum), RG-03 (type obligatoire)
 */
import {computed, watch} from 'vue';
import {type MaterialInput, type MaterialType, serialNumberMaxLength} from '../../../models/material.model.ts';
import type {SelectOption} from '../../../shared/composables/use-listbox.ts';
import {useMaterialTypes} from '../../../shared/composables/use-material-types.ts';
import AppSelect from '../../../shared/components/app-select.vue';

//------------------ PROPS / EMITS ---------------------------
/** Brouillon du formulaire, partagé avec material-form.vue. */
const draft = defineModel<MaterialInput>({required: true});

//------------------ DATAS -----------------------------------
const {materialTypes} = useMaterialTypes();

//------------------ COMPUTED --------------------------------
/** RG-03 : le type est choisi parmi ceux de l'API (enum EMaterialType du backend), avec leur libellé. */
const typeOptions = computed<SelectOption<MaterialType>[]>(() => materialTypes.value.map(type => ({value: type.code, label: type.label})));

//------------------ LIFECYCLE -------------------------------
// Création : sans type choisi, le premier type de l'API est proposé par défaut.
watch([materialTypes, () => draft.value.materialType], ([types, current]) => {
  if (current === '' && types.length > 0) draft.value.materialType = types[0].code;
}, {immediate: true});
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
