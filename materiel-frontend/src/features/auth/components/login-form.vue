<script setup lang="ts">
/**
 * Formulaire d'identification de l'écran MinDef Connect simulé (identifiant + mot de passe),
 * avec « Afficher » pour lire le mot de passe saisi et le lien « Mot de passe oublié ? ».
 * Composant de présentation : il émet les identifiants, la vue appelle le service.
 * La mise en page vient de style.css (MINDEF CONNECT SIMULÉ).
 * Utilisé par : features/auth/views/mindef-connect-view.vue (écran MinDef Connect simulé)
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
  /** L'utilisateur a cliqué sur « Mot de passe oublié ? ». */
  forgotPassword: [];
}>();

//------------------ DATAS -----------------------------------
const username = ref('');
const password = ref('');
/** Vrai si le mot de passe est affiché en clair (case « Afficher »). */
const showPassword = ref(false);

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
  <form class="sso-form" @submit.prevent="submit">
    <div class="field">
      <label for="login-username">Identifiant</label>
      <input
          id="login-username"
          v-model="username"
          autocomplete="username"
          placeholder="Veuillez entrer votre identifiant."
          required
      >
    </div>
    <div class="field">
      <label for="login-password">Mot de passe</label>
      <input
          id="login-password"
          v-model="password"
          :type="showPassword ? 'text' : 'password'"
          autocomplete="current-password"
          placeholder="Veuillez indiquer votre mot de passe."
          required
      >
    </div>
    <label class="sso-check">
      <input v-model="showPassword" type="checkbox">
      Afficher
    </label>
    <button
        type="button"
        class="sso-link"
        @click="emit('forgotPassword')"
    >
      Mot de passe oublié ?
    </button>
    <FormError :message="errorMessage" />
    <button type="submit" :disabled="!canSubmit">Se connecter</button>
  </form>
</template>
