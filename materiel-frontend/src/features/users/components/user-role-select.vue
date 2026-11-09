<script setup lang="ts">
/**
 * Liste déroulante des rôles, affichés par leur libellé (jamais par leur code).
 * Utilisé par : user-create-form.vue, user-edit-form.vue
 * RG : RG-14 (trois rôles)
 */
import {type Role, roleLabels, roles} from '../../../models/user.model.ts';
import type {SelectOption} from '../../../shared/composables/use-listbox.ts';
import AppSelect from '../../../shared/components/app-select.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** id de la liste, relié au <label for> du parent. */
  id: string;
}>();

/** Rôle choisi. */
const role = defineModel<Role>({required: true});

//------------------ DATAS -----------------------------------
/** Les trois rôles, dans l'ordre de roles (formateur, gestionnaire, administrateur). */
const options: SelectOption<Role>[] = roles.map(value => ({value, label: roleLabels[value]}));
</script>

<template>
  <AppSelect
      :id="id"
      v-model="role"
      :options="options"
  />
</template>
