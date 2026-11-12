<script setup lang="ts">
/**
 * OUTIL DE DÉMONSTRATION : connexion par rôle (un bouton par compte de démonstration), pour une
 * présentation. La page de Keycloak s'ouvre avec l'identifiant déjà rempli ; il reste le mot de passe.
 * Affiché seulement en mode démonstration (login-view.vue).
 * Composant de présentation : émet l'identifiant choisi, la page de connexion ouvre Keycloak.
 * Utilisé par : login-view.vue
 */
import {roleLabels} from '../../../models/user.model.ts';
import {demoAccounts} from '../../../shared/composables/use-dev-accounts.ts';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Vrai pendant l'ouverture de Keycloak, pour éviter les doubles clics. */
  submitting: boolean;
}>();

const emit = defineEmits<{
  /** Connexion demandée avec ce compte. */
  select: [username: string];
}>();

//------------------ DATAS -----------------------------------
/** Un bouton par compte, désigné par son rôle. */
const buttons = demoAccounts.map(account => ({...account, label: roleLabels[account.role]}));
</script>

<template>
  <div class="demo-login">
    <p class="demo-login-title">Démonstration : se connecter en tant que</p>
    <div class="demo-login-buttons">
      <button
          v-for="account in buttons"
          :key="account.username"
          type="button"
          class="secondary-button"
          :disabled="submitting"
          @click="emit('select', account.username)"
      >
        {{ account.label }}
      </button>
    </div>
  </div>
</template>
