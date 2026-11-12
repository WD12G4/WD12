<script setup lang="ts">
/**
 * Page de connexion. Seule page accessible sans être connecté, avec MinDef Connect (meta.public).
 * L'application ne demande pas elle-même le mot de passe : elle renvoie vers MinDef Connect,
 * l'authentification unique du ministère, ici simulée (mindef-connect-view.vue).
 * La page demandée avant la connexion (?redirect=) suit l'utilisateur jusqu'à MinDef Connect, qui
 * l'y renvoie une fois identifié (navigation.ts, navigateAfterLogin).
 * Après une session expirée (?session=expiree, voir router/guards.ts), un message l'explique.
 * RG : RG-14 (rôles liés à des comptes)
 */
import {computed} from 'vue';
import {useRoute} from 'vue-router';
import type {RouteTarget} from '../../../router/route-types.ts';
import Card from '../../../shared/components/card.vue';
import Link from '../../../shared/components/Link.vue';
import mindefConnectLogo from '../../../assets/mindef-connect-logo.png';

//------------------ DATAS -----------------------------------
const route = useRoute();

//------------------ COMPUTED --------------------------------
const sessionExpired = computed(() => route.query.session === 'expiree');
/** Lien vers MinDef Connect, qui garde la page demandée pour y revenir après l'identification. */
const ssoTarget = computed<RouteTarget>(() => {
  const redirect = route.query.redirect;
  return typeof redirect === 'string'
      ? {name: 'mindef-connect', query: {redirect}}
      : {name: 'mindef-connect'};
});
</script>

<template>
  <!-- Marque -->
  <div class="login-brand">
    <span class="brand-logo" aria-hidden="true">B</span>
    <h1>BASTION</h1>
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
      <p>L'accès se fait avec MinDef Connect, l'authentification unique du ministère.</p>
    </template>
    <template #body>
      <!-- Bouton « S'identifier avec MinDefConnect » : logo, libellé sur deux lignes -->
      <Link class="sso-button" :to="ssoTarget">
        <img
            class="sso-button-logo"
            :src="mindefConnectLogo"
            alt=""
        >
        <span class="sso-button-text">
          S'identifier avec
          <strong>MinDefConnect</strong>
        </span>
      </Link>
    </template>
  </Card>
</template>
