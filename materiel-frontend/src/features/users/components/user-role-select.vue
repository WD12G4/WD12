<script setup lang="ts">
/**
 * Liste déroulante des rôles, affichés par leur libellé (jamais par leur code).
 * Utilisé par : user-create-form.vue, user-edit-form.vue
 * RG : RG-14 (trois rôles)
 */
import {type Role, roleLabels, roles} from '../../../models/user.model.ts';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** id de la balise <select>, relié au <label for> du parent. */
  id: string;
}>();

/** Rôle choisi. */
const role = defineModel<Role>({required: true});
</script>

<template>
  <select
      :id="id"
      v-model="role"
      required
  >
    <option
        v-for="value in roles"
        :key="value"
        :value="value"
    >
      {{ roleLabels[value] }}
    </option>
  </select>
</template>
