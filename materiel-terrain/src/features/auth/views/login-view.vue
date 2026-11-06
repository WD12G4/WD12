<script setup lang="ts">
/**
 * Connexion à la PWA terrain, avec les comptes du backend existant.
 * RG : RG-14 (rôles liés à des comptes), RG-19 (un compte désactivé est refusé par le backend)
 */
import {useRouter} from 'vue-router';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {usePwaInstall} from '../../../shared/composables/use-pwa-install.ts';
import {ApiError} from '../../../services/api-error.ts';
import LoginForm from '../components/login-form.vue';

//------------------ DATAS -----------------------------------
const router = useRouter();
const {login} = useAuth();
const {loading, errorMessage, run} = useAsyncState();
const {isInstallable, isInstalled, isIos, install} = usePwaInstall();

//------------------ FUNCTIONS -------------------------------
/**
 * Connecte l'agent puis ouvre le scan.
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
    await router.push({name: 'scan'});
  }
}
</script>

<template>
  <div class="login">
    <div class="login-brand">
      <span class="insignia" aria-hidden="true"><span>⌖</span></span>
      <p class="mono small">ORDRE D'OPÉRATION — RECON</p>
      <h1>RECON</h1>
      <p>Identifier. Localiser. Reprendre le contrôle.</p>
    </div>
    <section class="panel">
      <LoginForm :submitting="loading" :error-message="errorMessage" @submit="signIn" />
    </section>
    <div v-if="isInstallable && !isInstalled" style="margin-top: 1rem; text-align: center;">
      <button type="button" class="btn secondary small" @click="install">
        📲 Installer l'application sur l'appareil
      </button>
    </div>
    <p v-else-if="isIos && !isInstalled" class="small muted" style="margin-top: 1rem; text-align: center;">
      💡 Sur iPhone : appuyez sur Partager ⎋ puis « Sur l'écran d'accueil »
    </p>
  </div>
</template>
