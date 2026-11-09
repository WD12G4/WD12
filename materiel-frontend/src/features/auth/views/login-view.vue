<script setup lang="ts">
/**
 * Page de connexion. Seule page accessible sans être connecté (meta.public).
 * RG : RG-14 (rôles liés à des comptes), RG-19 (un compte désactivé est refusé par le backend)
 */
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {ApiError} from '../../../services/api-error.ts';
import {navigateAfterLogin} from '../../../router/navigation.ts';
import Card from '../../../shared/components/card.vue';
import LoginForm from '../components/login-form.vue';

//------------------ DATAS -----------------------------------
const {login} = useAuth();
const {loading, errorMessage, run} = useAsyncState();

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
      <p>Connectez-vous avec le compte fourni par l'administrateur.</p>
    </template>
    <template #body>
      <LoginForm
          :submitting="loading"
          :error-message="errorMessage"
          @submit="signIn"
      />
    </template>
  </Card>
</template>
