<script setup lang="ts">
/**
 * OUTIL DE DÉMONSTRATION : connexion en un clic avec un compte de démonstration (un bouton par rôle),
 * pour une présentation. Affiché seulement en mode démonstration (mindef-connect-view.vue).
 * Composant de présentation : émet les identifiants, l'écran MinDef Connect simulé se connecte.
 * Utilisé par : mindef-connect-view.vue
 */
import {roleLabels} from '../../../models/user.model.ts';
import {demoAccounts} from '../../../shared/composables/use-dev-accounts.ts';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Vrai pendant la connexion, pour éviter les doubles clics. */
  submitting: boolean;
}>();

const emit = defineEmits<{
  /** Connexion demandée avec ce compte. */
  submit: [username: string, password: string];
}>();

//------------------ DATAS -----------------------------------
/** Un bouton par compte, désigné par son rôle. */
const buttons = demoAccounts.map(account => ({...account, label: roleLabels[account.role]}));
</script>

<template>
  <div class="demo-login">
    <p class="demo-login-title">Démonstration : se connecter en un clic</p>
    <div class="demo-login-buttons">
      <button
          v-for="account in buttons"
          :key="account.username"
          type="button"
          class="secondary-button"
          :disabled="submitting"
          @click="emit('submit', account.username, account.password)"
      >
        {{ account.label }}
      </button>
    </div>
  </div>
</template>
