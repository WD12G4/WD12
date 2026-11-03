<script setup lang="ts">
/**
 * Formulaire de modification d'un compte : rôle, activation et nouveau mot de passe (ADMIN).
 * Un mot de passe laissé vide n'est pas modifié.
 * Utilisé par : user-view.vue
 * RG : RG-15, RG-19 (un compte désactivé ne peut plus se connecter), RG-20, RG-32
 */
import {computed, ref, watch} from 'vue';
import {passwordLength, type User, type UserUpdateInput} from '../../../models/user.model.ts';
import FormError from '../../../shared/components/form-error.vue';
import UserRoleSelect from './user-role-select.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Compte à modifier. */
  user: User;
  /** Vrai pendant l'envoi. */
  submitting: boolean;
  /** Erreur renvoyée par l'API, ou null. */
  errorMessage: string | null;
}>();

const emit = defineEmits<{
  /** Saisie validée. */
  submit: [input: UserUpdateInput];
  /** Abandon de la modification. */
  cancel: [];
}>();

//------------------ DATAS -----------------------------------
const draft = ref<UserUpdateInput>({password: '', role: 'USER', enabled: true});

//------------------ COMPUTED --------------------------------
/** RG-20 : le mot de passe est facultatif, mais s'il est saisi il fait au moins 6 caractères. */
const isValid = computed(() => {
  const length = draft.value.password.length;
  return length === 0 || length >= passwordLength.min;
});
/** Le bouton d'envoi est actif si la saisie est valide et qu'aucun envoi n'est en cours. */
const canSubmit = computed(() => isValid.value && !props.submitting);

//------------------ FUNCTIONS -------------------------------
/** Émet la saisie si elle est valide. */
function submit(): void {
  if (!isValid.value) return;
  emit('submit', {...draft.value});
}

//------------------ LIFECYCLE -------------------------------
watch(() => props.user, user => {
  draft.value = {password: '', role: user.role, enabled: user.enabled};
}, {immediate: true});
</script>

<template>
  <form class="form-grid" @submit.prevent="submit">
    <div class="field">
      <label for="edit-role">Rôle</label>
      <UserRoleSelect id="edit-role" v-model="draft.role" />
    </div>
    <div class="field">
      <label for="edit-password">Nouveau mot de passe <span class="field-hint">(laisser vide pour ne pas changer)</span></label>
      <input
          id="edit-password"
          v-model="draft.password"
          type="password"
          :maxlength="passwordLength.max"
          autocomplete="new-password"
      >
    </div>
    <label class="checkbox-field full-width">
      <input v-model="draft.enabled" type="checkbox">
      Compte actif (un compte désactivé ne peut plus se connecter)
    </label>
    <div class="full-width">
      <FormError :message="errorMessage" />
      <div class="actions">
        <button type="submit" :disabled="!canSubmit">Enregistrer</button>
        <button
            type="button"
            class="secondary-button"
            @click="emit('cancel')"
        >
          Annuler
        </button>
      </div>
    </div>
  </form>
</template>
