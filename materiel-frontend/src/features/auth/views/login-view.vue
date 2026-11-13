<script setup lang="ts">
/**
 * Page de connexion. Seule page accessible sans être connecté (meta.public).
 * L'application ne demande pas elle-même le mot de passe : « S'identifier avec MinDefConnect » ouvre la
 * page de Keycloak, qui joue MinDef Connect (services/keycloak.ts). Une fois identifié, Keycloak ramène
 * sur la page demandée avant la connexion (?redirect=, afterLoginHref de navigation.ts).
 * Messages : session expirée (?session=expiree, voir router/guards.ts), compte refusé par l'application
 * (inconnu ou désactivé, RG-19), Keycloak injoignable.
 * En mode démonstration, un bouton par rôle ouvre Keycloak avec l'identifiant prérempli (demo-login.vue).
 * Mode démonstration : actif en développement et, par défaut, dans la version construite (présentation
 * sur la VM). Pour une vraie mise en production, construire avec VITE_DEMO_ACCOUNTS=false : les boutons
 * de démonstration sont alors retirés du build (condition écrite telle quelle pour que Vite la remplace
 * par une constante).
 * RG : RG-14 (rôles liés à des comptes), RG-19 (un compte désactivé est refusé par le backend)
 */
import {computed, defineAsyncComponent, ref} from 'vue';
import {useRoute} from 'vue-router';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {afterLoginHref} from '../../../router/navigation.ts';
import BrandMark from '../../../shared/components/brand-mark.vue';
import BrandWordmark from '../../../shared/components/brand-wordmark.vue';
import Card from '../../../shared/components/card.vue';
import mindefConnectLogo from '../../../assets/mindef-connect-logo.png';

//------------------ DATAS -----------------------------------
const {login, ssoUnavailable, accountRefused} = useAuth();
const route = useRoute();
/** Vrai pendant l'ouverture de Keycloak (la page est quittée). */
const redirecting = ref(false);
/** Connexion par rôle : null hors mode démonstration (import retiré du build). */
const DemoLogin = import.meta.env.DEV || import.meta.env.VITE_DEMO_ACCOUNTS !== 'false'
    ? defineAsyncComponent(() => import('../components/demo-login.vue')) : null;

//------------------ COMPUTED --------------------------------
const sessionExpired = computed(() => route.query.session === 'expiree');

//------------------ FUNCTIONS -------------------------------
/**
 * Ouvre la page de connexion de Keycloak, qui ramènera sur la page demandée.
 * @param username identifiant prérempli (mode démonstration)
 */
async function signIn(username?: string): Promise<void> {
  redirecting.value = true;
  try {
    await login(afterLoginHref(), username);
  } catch {
    redirecting.value = false;
    ssoUnavailable.value = true;
  }
}
</script>

<template>
  <!-- Marque -->
  <div class="login-brand">
    <BrandMark class="brand-logo" />
    <h1><BrandWordmark class="login-wordmark" /></h1>
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
      <p
          v-if="accountRefused"
          class="flash error"
          role="alert"
      >
        MinDef Connect vous a identifié, mais votre compte n'a pas accès à BASTION
        (compte inconnu ou désactivé). Connectez-vous avec un autre compte ou contactez l'administrateur.
      </p>
      <p
          v-if="ssoUnavailable"
          class="flash error"
          role="alert"
      >
        MinDef Connect ne répond pas. Rechargez la page dans un instant.
      </p>
      <p>L'accès se fait avec MinDef Connect, l'authentification unique du ministère.</p>
    </template>
    <template #body>
      <!-- Bouton « S'identifier avec MinDefConnect » : logo, libellé sur deux lignes -->
      <button
          type="button"
          class="sso-button"
          :disabled="redirecting || ssoUnavailable"
          @click="signIn()"
      >
        <img
            class="sso-button-logo"
            :src="mindefConnectLogo"
            alt=""
        >
        <span class="sso-button-text">
          S'identifier avec
          <strong>MinDefConnect</strong>
        </span>
      </button>
      <component
          :is="DemoLogin"
          v-if="DemoLogin && !ssoUnavailable"
          :submitting="redirecting"
          @select="signIn"
      />
    </template>
  </Card>
</template>
