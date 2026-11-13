<script setup lang="ts">
/**
 * Formulaire de modification d'un compte : rôle et activation (ADMIN).
 * Le mot de passe se gère dans Keycloak (MinDef Connect), pas dans l'application.
 * Utilisé par : user-view.vue
 * RG : RG-15, RG-19 (un compte désactivé ne peut plus se connecter), RG-32
 */
import {computed, ref, watch} from 'vue';
import {type User, type UserUpdateInput} from '../../../models/user.model.ts';
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
const draft = ref<UserUpdateInput>({role: 'USER', enabled: true});

//------------------ COMPUTED --------------------------------
/** Le bouton d'envoi est actif si aucun envoi n'est en cours. */
const canSubmit = computed(() => !props.submitting);

//------------------ FUNCTIONS -------------------------------
/** Émet la saisie. */
function submit(): void {
  emit('submit', {...draft.value});
}

//------------------ LIFECYCLE -------------------------------
watch(() => props.user, user => {
  draft.value = {role: user.role, enabled: user.enabled};
}, {immediate: true});
</script>

<template>
  <form class="form-grid" @submit.prevent="submit">
    <div class="field">
      <label for="edit-role">Rôle</label>
      <UserRoleSelect id="edit-role" v-model="draft.role" />
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
