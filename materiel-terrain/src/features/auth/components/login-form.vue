<script setup lang="ts">
/**
 * Formulaire de connexion (identifiant + mot de passe).
 * Composant de présentation : il émet les identifiants, la vue appelle le service.
 * Utilisé par : features/auth/views/login-view.vue
 */
import {computed, ref} from 'vue';
import FormError from '../../../shared/components/form-error.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Vrai pendant l'envoi : le bouton est désactivé pour éviter un double envoi. */
  submitting: boolean;
  /** Erreur renvoyée par l'API (identifiants refusés…), ou null. */
  errorMessage: string | null;
}>();

const emit = defineEmits<{
  /** Identifiants saisis. */
  submit: [username: string, password: string];
}>();

//------------------ DATAS -----------------------------------
const username = ref('');
const password = ref('');

//------------------ COMPUTED --------------------------------
const isValid = computed(() => username.value.trim().length > 0 && password.value.length > 0);
/** Le bouton d'envoi est actif si la saisie est valide et qu'aucun envoi n'est en cours. */
const canSubmit = computed(() => isValid.value && !props.submitting);

//------------------ FUNCTIONS -------------------------------
/** Envoie les identifiants si les deux champs sont remplis. */
function submit(): void {
  if (!isValid.value) return;
  emit('submit', username.value.trim(), password.value);
}
</script>

<template>
  <form class="form-grid" @submit.prevent="submit">
    <div class="field">
      <label for="login-username">Identifiant</label>
      <input
          id="login-username"
          v-model="username"
          autocomplete="username"
          required
      >
    </div>
    <div class="field">
      <label for="login-password">Mot de passe</label>
      <input
          id="login-password"
          v-model="password"
          type="password"
          autocomplete="current-password"
          required
      >
    </div>
    <div class="full-width">
      <FormError :message="errorMessage" />
      <button type="submit" :disabled="!canSubmit">Se connecter</button>
    </div>
  </form>
</template>
