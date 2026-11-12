<script setup lang="ts">
/**
 * Page de connexion. Seule page accessible sans être connecté (meta.public).
 * Après une session expirée (?session=expiree, voir router/guards.ts), un message l'explique.
 * En mode démonstration, un bouton par rôle connecte en un clic (demo-login.vue).
 * Mode démonstration : actif en développement et, par défaut, dans la version construite (présentation
 * sur la VM). Pour une vraie mise en production, construire avec VITE_DEMO_ACCOUNTS=false : les comptes
 * de démonstration et leurs mots de passe sont alors retirés du build (condition écrite telle quelle
 * pour que Vite la remplace par une constante).
 * RG : RG-14 (rôles liés à des comptes), RG-19 (un compte désactivé est refusé par le backend)
 */
import {computed, defineAsyncComponent} from 'vue';
import {useRoute} from 'vue-router';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {ApiError} from '../../../services/api-error.ts';
import {navigateAfterLogin} from '../../../router/navigation.ts';
import Card from '../../../shared/components/card.vue';
import LoginForm from '../components/login-form.vue';

//------------------ DATAS -----------------------------------
const {login} = useAuth();
const {loading, errorMessage, run} = useAsyncState();
const route = useRoute();
/** Connexion en un clic : null hors mode démonstration (import retiré du build). */
const DemoLogin = import.meta.env.DEV || import.meta.env.VITE_DEMO_ACCOUNTS !== 'false'
    ? defineAsyncComponent(() => import('../components/demo-login.vue')) : null;

//------------------ COMPUTED --------------------------------
const sessionExpired = computed(() => route.query.session === 'expiree');

//------------------ FUNCTIONS -------------------------------
/**
 * Connecte l'utilisateur puis l'envoie au tableau de bord.
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
  <!-- Marque -->
  <div class="login-brand">
    <span class="brand-logo" aria-hidden="true">GP</span>
    <h1>Gestion du parc</h1>
    <p>Le matériel informatique et les salles de l'école, au même endroit.</p>
  </div>

  <Card title="Connexion">
    <template #header>
      <p
          v-if="sessionExpired"
          class="flash error"
          role="alert"
      >
        Votre session a expiré. Reconnectez-vous : vous reviendrez sur la page demandée.
      </p>
      <p>Connectez-vous avec le compte fourni par l'administrateur.</p>
    </template>
    <template #body>
      <LoginForm
          :submitting="loading"
          :error-message="errorMessage"
          @submit="signIn"
      />
      <component
          :is="DemoLogin"
          v-if="DemoLogin"
          :submitting="loading"
          @submit="signIn"
      />
    </template>
  </Card>
</template>
