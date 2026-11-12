<script setup lang="ts">
/**
 * SIMULATION : écran d'identification de MinDef Connect (authentification unique du ministère).
 * Il imite le passage par le fournisseur d'identité : la page de connexion y renvoie, l'utilisateur
 * s'identifie, puis il revient dans l'application, sur la page demandée (?redirect=, navigateAfterLogin).
 * Aucun vrai service n'est appelé : les identifiants restent ceux des comptes de l'application,
 * vérifiés par le backend (/auth/login/), et l'écran est marqué « Simulation ».
 * « Mot de passe oublié ? » et « Créer un compte » n'existent pas dans la simulation : ils rappellent
 * que les comptes sont gérés par l'administrateur (RG-14).
 * Présentation : système de design de l'État (DSFR), quel que soit le thème choisi dans l'application
 * (data-theme="dsfr" sur la page) ; en-tête avec le bloc-marque et un avertissement « écran simulé »
 * bien visible, pour ne jamais le confondre avec le vrai service.
 * Mode démonstration (VITE_DEMO_ACCOUNTS ≠ false) : un bouton par rôle connecte en un clic (demo-login.vue).
 * RG : RG-14 (rôles liés à des comptes), RG-19 (un compte désactivé est refusé par le backend)
 */
import {computed, defineAsyncComponent} from 'vue';
import {useRoute} from 'vue-router';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useFlashMessage} from '../../../shared/composables/use-flash-message.ts';
import {ApiError} from '../../../services/api-error.ts';
import {navigateAfterLogin} from '../../../router/navigation.ts';
import type {RouteTarget} from '../../../router/route-types.ts';
import Link from '../../../shared/components/Link.vue';
import LoginForm from '../components/login-form.vue';

//------------------ DATAS -----------------------------------
const {login} = useAuth();
const {loading, errorMessage, run} = useAsyncState();
const route = useRoute();
const {show} = useFlashMessage();
/** Connexion en un clic : null hors mode démonstration (import retiré du build). */
const DemoLogin = import.meta.env.DEV || import.meta.env.VITE_DEMO_ACCOUNTS !== 'false'
    ? defineAsyncComponent(() => import('../components/demo-login.vue')) : null;

//------------------ COMPUTED --------------------------------
/** « Annuler » ramène à la page de connexion de l'application, avec la page demandée. */
const cancelTarget = computed<RouteTarget>(() => {
  const redirect = route.query.redirect;
  return typeof redirect === 'string' ? {name: 'login', query: {redirect}} : {name: 'login'};
});

//------------------ FUNCTIONS -------------------------------
/** « Mot de passe oublié ? » et « Créer un compte » : les comptes sont gérés par l'administrateur. */
function explainAccounts(): void {
  show("Simulation : les comptes et les mots de passe sont gérés par l'administrateur de Gestion du parc.");
}

/**
 * Identifie l'utilisateur puis le renvoie dans l'application, sur la page demandée.
 * Une 401 est traduite en message clair : le backend renvoie un corps vide dans ce cas.
 */
async function signIn(username: string, password: string): Promise<void> {
  const succeeded = await run(async () => {
    try {
      await login(username, password);
    } catch (error) {
      throw error instanceof ApiError && error.status === 401
          ? new ApiError(400, 'Identifiant ou mot de passe incorrect, ou compte désactivé.')
          : error;
    }
  });
  if (succeeded) {
    await navigateAfterLogin();
  }
}
</script>

<template>
  <!-- Écran du fournisseur d'identité (simulé) : page DSFR, hors thème de l'application -->
  <div class="sso-page" data-theme="dsfr">
    <header class="sso-header">
      <p class="fr-logo">République<br>Française</p>
      <p class="sso-service">
        <strong>MinDef Connect</strong>
        <span class="sim-badge">Simulation</span>
      </p>
    </header>
    <p class="sso-sim-notice" role="note">
      Écran simulé pour la démonstration de Gestion du parc : ce n'est pas le service MinDef Connect.
    </p>
    <section class="sso-panel" aria-labelledby="sso-title">
      <h1 id="sso-title">MindefConnect</h1>
      <h2>Se connecter avec son compte</h2>
      <p class="sso-intro">
        <strong>Gestion du parc</strong> demande à vous identifier.
      </p>

      <LoginForm
          :submitting="loading"
          :error-message="errorMessage"
          @submit="signIn"
          @forgot-password="explainAccounts"
      />

      <!-- Séparateur « Ou » -->
      <p class="sso-divider"><span>Ou</span></p>

      <h2>Vous n'avez pas de compte ?</h2>
      <button
          type="button"
          class="sso-outline-button"
          @click="explainAccounts"
      >
        Créer un compte
      </button>

      <component
          :is="DemoLogin"
          v-if="DemoLogin"
          :submitting="loading"
          @submit="signIn"
      />

      <p class="sso-cancel">
        <Link :to="cancelTarget">Annuler et revenir à Gestion du parc</Link>
      </p>
    </section>
  </div>
</template>
