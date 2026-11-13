<script setup lang="ts">
/**
 * Formulaire de création d'un compte (ADMIN) : identifiant et rôle.
 * La connexion passe par Keycloak (MinDef Connect) : le compte doit y exister avec le même identifiant,
 * l'application lui attribue seulement son rôle. Pas de mot de passe ici.
 * Composant de présentation : il émet la saisie, la vue appelle le service.
 * Utilisé par : user-view.vue
 * RG : RG-14, RG-20 (identifiant unique), RG-32
 */
import {computed, ref} from 'vue';
import {type UserCreateInput, usernameMaxLength} from '../../../models/user.model.ts';
import FormError from '../../../shared/components/form-error.vue';
import UserRoleSelect from './user-role-select.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Vrai pendant l'envoi. */
  submitting: boolean;
  /** Erreur renvoyée par l'API (identifiant déjà pris…), ou null. */
  errorMessage: string | null;
}>();

const emit = defineEmits<{
  /** Saisie validée. */
  submit: [input: UserCreateInput];
}>();

//------------------ DATAS -----------------------------------
const defaultUser: Readonly<UserCreateInput> = {username: '', role: 'USER'};
const draft = ref<UserCreateInput>({...defaultUser});

//------------------ COMPUTED --------------------------------
/** RG-20 : identifiant renseigné. */
const isValid = computed(() => draft.value.username.trim().length > 0);
/** Le bouton d'envoi est actif si la saisie est valide et qu'aucun envoi n'est en cours. */
const canSubmit = computed(() => isValid.value && !props.submitting);

//------------------ FUNCTIONS -------------------------------
/**
 * Émet la saisie. Le formulaire n'est pas vidé ici : en cas d'erreur de l'API, la saisie est gardée.
 * La vue le réinitialise après un succès (changement de :key).
 */
function submit(): void {
  if (!isValid.value) return;
  emit('submit', {...draft.value});
}
</script>

<template>
  <form class="form-grid" @submit.prevent="submit">
    <div class="field">
      <label for="user-username">Identifiant <span class="field-hint">(le même que dans MinDef Connect)</span></label>
      <input
          id="user-username"
          v-model="draft.username"
          :maxlength="usernameMaxLength"
          autocomplete="off"
          required
      >
    </div>
    <div class="field">
      <label for="user-role">Rôle</label>
      <UserRoleSelect id="user-role" v-model="draft.role" />
    </div>
    <div class="full-width">
      <FormError :message="errorMessage" />
      <button type="submit" :disabled="!canSubmit">Créer le compte</button>
    </div>
  </form>
</template>
